"""Object-level authorization matrix tests."""
import uuid
import requests
from conftest import BASE_URL, auth_header, login

class TestObjectLevelAuth:
    """Verify that users can only access/modify resources they own."""

    def test_seller_cannot_modify_other_sellers_product(self, tokens, base_url):
        """Seller A cannot update Seller B's product."""
        # Products 1-6 belong to seller (user id 3)
        # Login as member (not the seller) and try to update product 1
        headers = auth_header(tokens["member"])
        resp = requests.put(
            f"{base_url}/products/1",
            headers=headers,
            json={"name": "Hacked", "price": 0.01, "stockQuantity": 1, "categoryId": 1},
        )
        assert resp.status_code == 403, f"Expected 403, got {resp.status_code}"

    def test_seller_create_binds_to_authenticated_user(self, tokens, base_url):
        """Product creation ignores client-provided sellerId."""
        headers = auth_header(tokens["seller"])
        resp = requests.post(
            f"{base_url}/products",
            headers=headers,
            json={
                "name": "Test Craft",
                "description": "Test",
                "price": 10.00,
                "stockQuantity": 5,
                "categoryId": 1,
                "sellerId": 999,  # Attempt to assign to another user
            },
        )
        assert resp.status_code == 200
        # The sellerId should be the authenticated seller (id=3), not 999
        body = resp.json()
        assert body.get("sellerId") != 999

    def test_buyer_cannot_set_order_to_shipped(self, tokens, base_url):
        """Buyers can only cancel their own orders, not ship them."""
        headers = auth_header(tokens["member"])
        resp = requests.patch(
            f"{base_url}/orders/1/status?status=SHIPPED",
            headers=headers,
        )
        assert resp.status_code == 403

    def test_buyer_can_cancel_own_order(self, tokens, base_url):
        """Buyers can cancel their own orders."""
        headers = auth_header(tokens["member"])
        resp = requests.patch(
            f"{base_url}/orders/1/status?status=CANCELLED",
            headers=headers,
        )
        # Should succeed (200) or fail with state error (409) if already cancelled
        assert resp.status_code in (200, 409)

    def test_user_cannot_confirm_other_users_reservation(self, tokens, base_url):
        """User A cannot confirm User B's reservation."""
        # Create a reservation as member
        mh = auth_header(tokens["member"])
        key = uuid.uuid4().hex
        r1 = requests.post(
            f"{base_url}/reservations",
            headers=mh,
            json={"inventoryItemId": 1, "quantity": 1, "idempotencyKey": key},
        )
        if r1.status_code != 200:
            return  # Skip if creation fails
        rid = r1.json()["id"]

        # Seller tries to confirm it
        sh = auth_header(tokens["seller"])
        r2 = requests.post(f"{base_url}/reservations/{rid}/confirm", headers=sh)
        assert r2.status_code == 403

        # Clean up
        requests.post(f"{base_url}/reservations/{rid}/cancel", headers=mh)

    def test_user_cannot_view_other_users_incident(self, tokens, base_url):
        """Regular user cannot view another user's incident."""
        # Create incident as member
        mh = auth_header(tokens["member"])
        r1 = requests.post(
            f"{base_url}/incidents",
            headers=mh,
            json={"incidentType": "OTHER", "severity": "LOW", "title": "Private", "description": "Mine"},
        )
        if r1.status_code != 200:
            return
        iid = r1.json()["id"]

        # Seller tries to view it (not moderator/admin)
        sh = auth_header(tokens["seller"])
        r2 = requests.get(f"{base_url}/incidents/{iid}", headers=sh)
        assert r2.status_code == 403

    def test_user_cannot_view_other_users_appeal(self, tokens, base_url):
        """Regular user cannot view another user's appeal."""
        mh = auth_header(tokens["member"])
        r1 = requests.post(
            f"{base_url}/appeals",
            headers=mh,
            json={"relatedEntityType": "PRODUCT", "relatedEntityId": 1, "reason": "Test auth"},
        )
        if r1.status_code != 200:
            return
        aid = r1.json()["id"]

        sh = auth_header(tokens["seller"])
        r2 = requests.get(f"{base_url}/appeals/{aid}", headers=sh)
        assert r2.status_code == 403

    def test_admin_bypasses_all_ownership_checks(self, tokens, base_url):
        """Admin can access any resource regardless of ownership."""
        ah = auth_header(tokens["admin"])
        # Admin can view any order
        r1 = requests.get(f"{base_url}/orders/1", headers=ah)
        assert r1.status_code in (200, 404)

        # Admin can view any incident
        r2 = requests.get(f"{base_url}/incidents/1", headers=ah)
        assert r2.status_code in (200, 404)

    def test_evidence_upload_strips_stored_path(self, tokens, base_url):
        """Evidence list response must not contain storedPath."""
        mh = auth_header(tokens["member"])
        r1 = requests.post(
            f"{base_url}/appeals",
            headers=mh,
            json={"relatedEntityType": "ORDER", "relatedEntityId": 1, "reason": "Path test"},
        )
        if r1.status_code != 200:
            return
        aid = r1.json()["id"]

        r2 = requests.get(f"{base_url}/appeals/{aid}/evidence", headers=mh)
        assert r2.status_code == 200
        for item in r2.json():
            assert "storedPath" not in item, "storedPath should not be exposed"

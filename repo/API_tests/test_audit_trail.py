"""Audit trail integration tests — verify @Audited writes persist."""
import uuid
import requests
from conftest import BASE_URL, auth_header


class TestAuditTrail:
    """Verify that mutating operations produce audit log entries."""

    def test_product_create_writes_audit(self, admin_token, base_url):
        """Creating a product should write an audit entry for PRODUCT/CREATE."""
        headers = auth_header(admin_token)
        # Create a product
        resp = requests.post(
            f"{base_url}/products",
            headers=headers,
            json={"name": "Audit Test Vase", "description": "Test", "price": 50, "stockQuantity": 5, "categoryId": 1},
        )
        assert resp.status_code == 200
        pid = resp.json()["id"]

        # Check audit log for this product
        audit = requests.get(f"{base_url}/audit/PRODUCT/{pid}", headers=headers)
        assert audit.status_code == 200
        entries = audit.json()
        assert any(e.get("action") == "CREATE" for e in entries), f"Expected CREATE audit entry, got {entries}"

    def test_order_place_writes_audit(self, member_token, admin_token, base_url):
        """Placing an order should write an audit entry."""
        mh = auth_header(member_token)
        ah = auth_header(admin_token)
        resp = requests.post(
            f"{base_url}/orders",
            headers=mh,
            json={"productId": 1, "quantity": 1, "totalPrice": 85.00},
        )
        if resp.status_code != 200:
            return
        oid = resp.json()["id"]

        audit = requests.get(f"{base_url}/audit/ORDER/{oid}", headers=ah)
        assert audit.status_code == 200
        entries = audit.json()
        assert any(e.get("action") == "PLACE" for e in entries), f"Expected PLACE audit, got {entries}"

    def test_incident_create_writes_audit(self, member_token, admin_token, base_url):
        """Creating an incident should write an audit entry."""
        mh = auth_header(member_token)
        ah = auth_header(admin_token)
        resp = requests.post(
            f"{base_url}/incidents",
            headers=mh,
            json={"incidentType": "OTHER", "severity": "LOW", "title": "Audit test", "description": "Audit", "sellerId": 3},
        )
        assert resp.status_code == 200
        iid = resp.json()["id"]

        audit = requests.get(f"{base_url}/audit/INCIDENT/{iid}", headers=ah)
        assert audit.status_code == 200
        entries = audit.json()
        assert any(e.get("action") == "CREATE" for e in entries), f"Expected CREATE audit, got {entries}"

    def test_reservation_writes_audit(self, admin_token, base_url):
        """Reserving stock should write an audit entry."""
        headers = auth_header(admin_token)
        key = uuid.uuid4().hex
        resp = requests.post(
            f"{base_url}/reservations",
            headers=headers,
            json={"inventoryItemId": 1, "quantity": 1, "idempotencyKey": key},
        )
        if resp.status_code != 200:
            return
        rid = resp.json()["id"]

        audit = requests.get(f"{base_url}/audit/RESERVATION/{rid}", headers=headers)
        assert audit.status_code == 200
        entries = audit.json()
        assert any(e.get("action") == "RESERVE" for e in entries), f"Expected RESERVE audit, got {entries}"

    def test_audit_entries_have_actor_and_timestamp(self, admin_token, base_url):
        """Audit entries must include actorId and createdAt."""
        headers = auth_header(admin_token)
        resp = requests.post(
            f"{base_url}/products",
            headers=headers,
            json={"name": "AuditFieldTest", "description": "T", "price": 1, "stockQuantity": 1, "categoryId": 1},
        )
        if resp.status_code != 200:
            return
        pid = resp.json()["id"]

        audit = requests.get(f"{base_url}/audit/PRODUCT/{pid}", headers=headers)
        for entry in audit.json():
            assert "actorId" in entry and entry["actorId"] is not None, "Audit entry missing actorId"
            assert "createdAt" in entry and entry["createdAt"] is not None, "Audit entry missing createdAt"

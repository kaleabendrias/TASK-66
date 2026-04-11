"""Hardening tests: rate limits, SLA, audit purge, inventory races."""
import uuid
import time
import requests
from conftest import BASE_URL, auth_header, login

class TestHardening:
    def test_rate_limit_lockout_returns_429_or_error(self, base_url):
        """After 60+ requests, rate limit kicks in."""
        # Use a fresh user to avoid contamination
        unique = uuid.uuid4().hex[:8]
        r = requests.post(f"{base_url}/auth/register", json={
            "username": f"ratelimit_{unique}", "email": f"rl_{unique}@test.com",
            "password": "password123", "displayName": "RL Test"
        })
        if r.status_code != 200:
            return
        token = r.json()["token"]
        headers = auth_header(token)

        # Make 65 rapid requests
        statuses = []
        for _ in range(65):
            resp = requests.get(f"{base_url}/categories", headers=headers)
            statuses.append(resp.status_code)

        assert 429 in statuses, "Expected at least one 429 in 65 requests"

    def test_order_transition_rejects_invalid(self, tokens, base_url):
        """Cannot skip order states (PLACED -> DELIVERED directly)."""
        headers = auth_header(tokens["admin"])
        # Place an order
        r = requests.post(f"{base_url}/orders", headers=headers,
                          json={"productId": 1, "quantity": 1, "totalPrice": 85.00})
        if r.status_code != 200:
            return
        oid = r.json()["id"]

        # Try to jump to DELIVERED
        r2 = requests.patch(f"{base_url}/orders/{oid}/status?status=DELIVERED", headers=headers)
        assert r2.status_code in (400, 409), f"Expected 400/409, got {r2.status_code}"

    def test_order_transition_idempotent(self, tokens, base_url):
        """Setting same status twice is idempotent."""
        headers = auth_header(tokens["admin"])
        r = requests.post(f"{base_url}/orders", headers=headers,
                          json={"productId": 1, "quantity": 1, "totalPrice": 85.00})
        if r.status_code != 200:
            return
        oid = r.json()["id"]

        r1 = requests.patch(f"{base_url}/orders/{oid}/status?status=CONFIRMED", headers=headers)
        r2 = requests.patch(f"{base_url}/orders/{oid}/status?status=CONFIRMED", headers=headers)
        assert r1.status_code == 200
        assert r2.status_code == 200

    def test_inventory_inbound_endpoint(self, tokens, base_url):
        """Dedicated inbound endpoint increases stock."""
        headers = auth_header(tokens["warehouse"])
        before = requests.get(f"{base_url}/inventory/product/1", headers=headers).json()
        initial_qty = before[0]["quantityOnHand"]
        item_id = before[0]["id"]

        r = requests.post(f"{base_url}/inventory/inbound", headers=headers,
                          json={"inventoryItemId": item_id, "quantity": 10, "referenceDocument": "PO-001"})
        assert r.status_code == 200
        assert r.json()["quantityOnHand"] == initial_qty + 10

    def test_inventory_outbound_endpoint(self, tokens, base_url):
        """Dedicated outbound endpoint decreases stock."""
        headers = auth_header(tokens["warehouse"])
        before = requests.get(f"{base_url}/inventory/product/1", headers=headers).json()
        initial_qty = before[0]["quantityOnHand"]
        item_id = before[0]["id"]

        r = requests.post(f"{base_url}/inventory/outbound", headers=headers,
                          json={"inventoryItemId": item_id, "quantity": 1, "referenceDocument": "SO-001"})
        assert r.status_code == 200
        assert r.json()["quantityOnHand"] == initial_qty - 1

    def test_inventory_stocktake_endpoint(self, tokens, base_url):
        """Stocktake sets absolute quantity."""
        headers = auth_header(tokens["warehouse"])
        r = requests.post(f"{base_url}/inventory/stocktake", headers=headers,
                          json={"productId": 1, "warehouseId": 1, "countedQuantity": 100, "referenceDocument": "ST-001"})
        assert r.status_code == 200
        assert r.json()["quantityOnHand"] == 100

    def test_inventory_dto_includes_warehouse_name(self, tokens, base_url):
        """Inventory DTOs must include warehouseName."""
        headers = auth_header(tokens["warehouse"])
        r = requests.get(f"{base_url}/inventory/product/1", headers=headers)
        assert r.status_code == 200
        for item in r.json():
            assert item.get("warehouseName") is not None, "warehouseName missing from DTO"

    def test_zero_quantity_reservation_rejected(self, tokens, base_url):
        """Zero quantity reservation must be rejected."""
        headers = auth_header(tokens["member"])
        r = requests.post(f"{base_url}/reservations", headers=headers,
                          json={"inventoryItemId": 1, "quantity": 0, "idempotencyKey": uuid.uuid4().hex})
        assert r.status_code == 400, f"Expected 400 for zero quantity, got {r.status_code}"

    def test_negative_quantity_reservation_rejected(self, tokens, base_url):
        """Negative quantity reservation must be rejected."""
        headers = auth_header(tokens["member"])
        r = requests.post(f"{base_url}/reservations", headers=headers,
                          json={"inventoryItemId": 1, "quantity": -5, "idempotencyKey": uuid.uuid4().hex})
        assert r.status_code == 400, f"Expected 400 for negative quantity, got {r.status_code}"

    def test_order_total_is_server_resolved_not_client_supplied(self, tokens, base_url):
        """The server must ignore client-supplied totalPrice and recompute it from product + tier benefits."""
        headers = auth_header(tokens["member"])
        # Product 1 in V2__seed_data is "Wireless Headphones" priced at 79.99.
        # We try to undercut it by 100x via the request body.
        r = requests.post(f"{base_url}/orders", headers=headers,
                          json={"productId": 1, "quantity": 1, "totalPrice": 0.01})
        assert r.status_code == 200, f"server-priced order should still place; got {r.status_code} {r.text}"
        body = r.json()
        # The exact total is product price minus any tier discounts the
        # member has access to. Whatever it is, it must NOT be the 0.01
        # number the client tried to inject.
        assert float(body["totalPrice"]) >= 1.0, \
            f"server total {body['totalPrice']} too low — client tampering not blocked"
        assert body.get("reservationId") is not None, \
            "every placed order must reference a stock reservation"

    def test_order_placement_creates_or_references_inventory_hold(self, tokens, base_url):
        """End-to-end: placing an order must produce a reservation visible via /reservations/my."""
        headers = auth_header(tokens["member"])
        # Snapshot existing held reservations.
        before = requests.get(f"{base_url}/reservations/my", headers=headers).json()
        before_ids = {r["id"] for r in before}

        r = requests.post(f"{base_url}/orders", headers=headers,
                          json={"productId": 1, "quantity": 1})
        assert r.status_code == 200, r.text
        reservation_id = r.json().get("reservationId")
        assert reservation_id is not None, "reservationId must be returned on placed order"

        after = requests.get(f"{base_url}/reservations/my", headers=headers).json()
        after_ids = {res["id"] for res in after}
        new_ids = after_ids - before_ids
        assert reservation_id in after_ids, \
            f"order's reservation {reservation_id} not visible in member's reservations: {sorted(after_ids)}"
        assert len(new_ids) >= 1, "expected at least one new reservation row after placing order"

    def test_cancelling_an_order_releases_its_reservation(self, tokens, base_url):
        """The compensation path must release the held quantity when the buyer cancels."""
        headers = auth_header(tokens["member"])
        place = requests.post(f"{base_url}/orders", headers=headers,
                              json={"productId": 2, "quantity": 1})
        assert place.status_code == 200, place.text
        order = place.json()
        oid = order["id"]
        reservation_id = order["reservationId"]
        assert reservation_id is not None

        cancel = requests.patch(f"{base_url}/orders/{oid}/status?status=CANCELLED", headers=headers)
        assert cancel.status_code == 200, cancel.text

        # The cancelled reservation must no longer appear in the member's
        # HELD reservation list.
        held = requests.get(f"{base_url}/reservations/my", headers=headers).json()
        held_ids = {r["id"] for r in held if r["status"] == "HELD"}
        assert reservation_id not in held_ids, \
            f"reservation {reservation_id} still HELD after order cancel: {held}"

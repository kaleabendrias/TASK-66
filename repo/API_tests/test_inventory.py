"""Inventory and warehouse tests."""
import uuid
import requests
from conftest import BASE_URL, auth_header


class TestInventory:
    def test_get_warehouses(self, warehouse_token, base_url):
        resp = requests.get(
            f"{base_url}/warehouses",
            headers=auth_header(warehouse_token),
        )
        assert resp.status_code == 200
        assert len(resp.json()) == 3

    def test_get_inventory_by_product(self, warehouse_token, base_url):
        resp = requests.get(
            f"{base_url}/inventory/product/1",
            headers=auth_header(warehouse_token),
        )
        assert resp.status_code == 200
        entries = resp.json()
        assert isinstance(entries, list)
        assert len(entries) > 0

    def test_low_stock_items(self, warehouse_token, base_url):
        resp = requests.get(
            f"{base_url}/inventory/low-stock",
            headers=auth_header(warehouse_token),
        )
        assert resp.status_code == 200
        assert isinstance(resp.json(), list)

    def test_stock_reservation_flow(self, warehouse_token, base_url):
        headers = auth_header(warehouse_token)
        key = uuid.uuid4().hex
        # Reserve
        resp = requests.post(
            f"{base_url}/reservations",
            json={"inventoryItemId": 1, "quantity": 1, "idempotencyKey": key},
            headers=headers,
        )
        assert resp.status_code in (200, 201)
        reservation = resp.json()
        rid = reservation["id"]
        assert reservation["status"] == "HELD"
        # Confirm
        resp = requests.post(
            f"{base_url}/reservations/{rid}/confirm",
            headers=headers,
        )
        assert resp.status_code == 200
        assert resp.json()["status"] == "CONFIRMED"

    def test_reservation_cancel_rollback(self, warehouse_token, base_url):
        headers = auth_header(warehouse_token)
        # Snapshot reserved BEFORE the reservation
        inv_before = requests.get(f"{base_url}/inventory/product/2", headers=headers).json()
        item_id = inv_before[0]["id"]
        reserved_before = inv_before[0]["quantityReserved"]

        key = uuid.uuid4().hex
        resp = requests.post(
            f"{base_url}/reservations",
            json={"inventoryItemId": item_id, "quantity": 1, "idempotencyKey": key},
            headers=headers,
        )
        assert resp.status_code in (200, 201)
        rid = resp.json()["id"]

        # Cancel immediately
        resp = requests.post(f"{base_url}/reservations/{rid}/cancel", headers=headers)
        assert resp.status_code == 200
        assert resp.json()["status"] == "CANCELLED"

        # Reserved quantity should be back to the before value
        inv_after = requests.get(f"{base_url}/inventory/product/2", headers=headers).json()
        assert inv_after[0]["quantityReserved"] == reserved_before

    def test_reservation_idempotency(self, warehouse_token, base_url):
        headers = auth_header(warehouse_token)
        key = uuid.uuid4().hex
        payload = {"inventoryItemId": 1, "quantity": 1, "idempotencyKey": key}
        r1 = requests.post(f"{base_url}/reservations", json=payload, headers=headers)
        r2 = requests.post(f"{base_url}/reservations", json=payload, headers=headers)
        assert r1.json()["id"] == r2.json()["id"]

    def test_adjust_stock(self, warehouse_token, base_url):
        headers = auth_header(warehouse_token)
        resp = requests.post(
            f"{base_url}/inventory/adjust",
            json={
                "inventoryItemId": 1,
                "quantityChange": 5,
                "movementType": "ADJUSTMENT",
                "referenceDocument": "TEST-ADJ-001",
                "notes": "Integration test adjustment",
            },
            headers=headers,
        )
        assert resp.status_code in (200, 201)

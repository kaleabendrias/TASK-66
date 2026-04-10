"""Cross-cutting idempotency tests."""
import uuid
import requests
from conftest import BASE_URL, auth_header


class TestIdempotency:
    def test_reservation_idempotency(self, warehouse_token, base_url):
        headers = auth_header(warehouse_token)
        key = uuid.uuid4().hex
        payload = {"inventoryItemId": 1, "quantity": 1, "idempotencyKey": key}
        r1 = requests.post(f"{base_url}/reservations", json=payload, headers=headers)
        r2 = requests.post(f"{base_url}/reservations", json=payload, headers=headers)
        assert r1.status_code in (200, 201)
        assert r2.status_code in (200, 201)
        assert r1.json()["id"] == r2.json()["id"]

    def test_fulfillment_idempotency(self, warehouse_token, base_url):
        headers = auth_header(warehouse_token)
        key = uuid.uuid4().hex
        payload = {"orderId": 1, "warehouseId": 1, "idempotencyKey": key}
        r1 = requests.post(f"{base_url}/fulfillments", json=payload, headers=headers)
        r2 = requests.post(f"{base_url}/fulfillments", json=payload, headers=headers)
        assert r1.status_code in (200, 201)
        assert r2.status_code in (200, 201)
        assert r1.json()["id"] == r2.json()["id"]

    def test_acknowledge_idempotency(self, moderator_token, base_url):
        headers = auth_header(moderator_token)
        # Create incident first
        resp = requests.post(
            f"{base_url}/incidents",
            json={
                "incidentType": "ORDER_ISSUE",
                "severity": "NORMAL",
                "title": "Idempotency test incident",
                "description": "Testing acknowledge idempotency",
            },
            headers=headers,
        )
        assert resp.status_code in (200, 201)
        iid = resp.json()["id"]

        # Acknowledge twice
        r1 = requests.post(f"{base_url}/incidents/{iid}/acknowledge", headers=headers)
        r2 = requests.post(f"{base_url}/incidents/{iid}/acknowledge", headers=headers)
        assert r1.status_code == 200
        assert r2.status_code == 200
        assert r1.json()["status"] == "ACKNOWLEDGED"
        assert r2.json()["status"] == "ACKNOWLEDGED"

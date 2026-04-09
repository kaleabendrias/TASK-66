"""Fulfillment workflow tests."""
import uuid
import requests
from conftest import BASE_URL, auth_header


class TestFulfillment:
    def test_create_fulfillment(self, warehouse_token, base_url):
        key = uuid.uuid4().hex
        resp = requests.post(
            f"{base_url}/fulfillments",
            json={"orderId": 1, "warehouseId": 1, "idempotencyKey": key},
            headers=auth_header(warehouse_token),
        )
        assert resp.status_code in (200, 201)
        body = resp.json()
        assert body["status"] == "PENDING"

    def test_advance_fulfillment_steps(self, warehouse_token, base_url):
        key = uuid.uuid4().hex
        headers = auth_header(warehouse_token)
        resp = requests.post(
            f"{base_url}/fulfillments",
            json={"orderId": 1, "warehouseId": 1, "idempotencyKey": key},
            headers=headers,
        )
        fid = resp.json()["id"]

        for step in ("PICK", "PACK", "SHIP", "DELIVER"):
            resp = requests.post(
                f"{base_url}/fulfillments/{fid}/advance",
                json={"stepName": step, "notes": f"Test {step}"},
                headers=headers,
            )
            assert resp.status_code == 200

    def test_fulfillment_idempotency(self, warehouse_token, base_url):
        key = uuid.uuid4().hex
        headers = auth_header(warehouse_token)
        payload = {"orderId": 1, "warehouseId": 1, "idempotencyKey": key}
        r1 = requests.post(f"{base_url}/fulfillments", json=payload, headers=headers)
        r2 = requests.post(f"{base_url}/fulfillments", json=payload, headers=headers)
        assert r1.json()["id"] == r2.json()["id"]

    def test_get_fulfillment_steps(self, warehouse_token, base_url):
        key = uuid.uuid4().hex
        headers = auth_header(warehouse_token)
        resp = requests.post(
            f"{base_url}/fulfillments",
            json={"orderId": 1, "warehouseId": 1, "idempotencyKey": key},
            headers=headers,
        )
        fid = resp.json()["id"]
        # Advance one step
        requests.post(
            f"{base_url}/fulfillments/{fid}/advance",
            json={"stepName": "PICK", "notes": "Picked"},
            headers=headers,
        )
        # Get steps
        resp = requests.get(
            f"{base_url}/fulfillments/{fid}/steps",
            headers=headers,
        )
        assert resp.status_code == 200
        steps = resp.json()
        assert isinstance(steps, list)
        assert len(steps) >= 1

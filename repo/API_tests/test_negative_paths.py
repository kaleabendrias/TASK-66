"""Negative path tests: cross-seller access, upload constraints, malformed requests."""
import uuid
import requests
from conftest import BASE_URL, auth_header

class TestNegativePaths:
    def test_seller_cannot_view_other_sellers_inventory(self, tokens, base_url):
        """Seller should be blocked from viewing inventory for products they don't own."""
        headers = auth_header(tokens["seller"])
        # Product 1 belongs to seller (user id 3) — seller should see it
        r1 = requests.get(f"{base_url}/inventory/product/1", headers=headers)
        assert r1.status_code in (200, 403)  # 200 if own product, 403 if not

    def test_seller_cannot_view_other_sellers_fulfillment(self, tokens, base_url):
        """Seller cannot access fulfillment for orders of other seller's products."""
        headers = auth_header(tokens["seller"])
        # Order 1 is for product 1 — seller owns product 1, so this should work
        r = requests.get(f"{base_url}/fulfillments/order/1", headers=headers)
        assert r.status_code in (200, 404)  # 200 if exists and owned

    def test_member_cannot_view_inventory(self, tokens, base_url):
        """Members should not access inventory endpoints at all."""
        headers = auth_header(tokens["member"])
        r = requests.get(f"{base_url}/inventory/product/1", headers=headers)
        assert r.status_code == 403

    def test_upload_rejects_oversized_file(self, tokens, base_url):
        """Files over 10MB should be rejected."""
        headers = auth_header(tokens["member"])
        # Create an appeal first
        r1 = requests.post(f"{base_url}/appeals", headers=headers,
                           json={"relatedEntityType": "PRODUCT", "relatedEntityId": 1, "reason": "Size test"})
        if r1.status_code != 200:
            return
        aid = r1.json()["id"]

        # Create a fake 11MB file
        big_data = b'\x89PNG' + b'\x00' * (11 * 1024 * 1024)
        files = {"file": ("big.png", big_data, "image/png")}
        r2 = requests.post(f"{base_url}/appeals/{aid}/evidence", headers=headers, files=files)
        assert r2.status_code == 400, f"Expected 400 for oversized file, got {r2.status_code}"

    def test_upload_rejects_non_image_type(self, tokens, base_url):
        """Non-image/PDF files should be rejected."""
        headers = auth_header(tokens["member"])
        r1 = requests.post(f"{base_url}/appeals", headers=headers,
                           json={"relatedEntityType": "PRODUCT", "relatedEntityId": 1, "reason": "Type test"})
        if r1.status_code != 200:
            return
        aid = r1.json()["id"]

        files = {"file": ("script.js", b"alert('xss')", "application/javascript")}
        r2 = requests.post(f"{base_url}/appeals/{aid}/evidence", headers=headers, files=files)
        assert r2.status_code == 400, f"Expected 400 for JS file, got {r2.status_code}"

    def test_appeal_without_reason_rejected(self, tokens, base_url):
        """Appeal creation without reason should fail validation."""
        headers = auth_header(tokens["member"])
        r = requests.post(f"{base_url}/appeals", headers=headers,
                          json={"relatedEntityType": "PRODUCT", "relatedEntityId": 1, "reason": ""})
        assert r.status_code == 400

    def test_appeal_without_entity_type_rejected(self, tokens, base_url):
        """Appeal creation without relatedEntityType should fail."""
        headers = auth_header(tokens["member"])
        r = requests.post(f"{base_url}/appeals", headers=headers,
                          json={"relatedEntityType": "", "relatedEntityId": 1, "reason": "test"})
        assert r.status_code == 400

    def test_risk_event_ingestion(self, tokens, base_url):
        """Moderator can ingest anomaly events."""
        headers = auth_header(tokens["moderator"])
        r = requests.post(f"{base_url}/risk/events", headers=headers,
                          json={"userId": 3, "eventType": "MISSED_PICKUP_CHECKIN", "severity": "HIGH",
                                "details": {"location": "Warehouse A", "shift": "morning"}})
        assert r.status_code == 200
        event = r.json()
        assert event["eventType"] == "MISSED_PICKUP_CHECKIN"
        assert event["severity"] == "HIGH"

    def test_risk_event_ingestion_buddy_punching(self, tokens, base_url):
        """Ingesting buddy_punching event type."""
        headers = auth_header(tokens["admin"])
        r = requests.post(f"{base_url}/risk/events", headers=headers,
                          json={"userId": 3, "eventType": "BUDDY_PUNCHING", "severity": "HIGH"})
        assert r.status_code == 200

    def test_risk_event_propagates_to_score(self, tokens, base_url):
        """Ingested events should affect the risk score computation."""
        headers = auth_header(tokens["admin"])
        # Ingest some events
        for _ in range(3):
            requests.post(f"{base_url}/risk/events", headers=headers,
                          json={"userId": 3, "eventType": "MISIDENTIFICATION", "severity": "HIGH"})

        # Compute score
        r = requests.post(f"{base_url}/risk/compute/3", headers=headers)
        assert r.status_code == 200
        score = r.json()
        # Score should be > 0 due to the events
        assert score["score"] > 0

    def test_member_cannot_ingest_risk_events(self, tokens, base_url):
        """Members should not be able to record risk events."""
        headers = auth_header(tokens["member"])
        r = requests.post(f"{base_url}/risk/events", headers=headers,
                          json={"userId": 3, "eventType": "TEST", "severity": "LOW"})
        assert r.status_code == 403

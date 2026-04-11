"""Incident management tests."""
import requests
from conftest import BASE_URL, auth_header


def _create_incident(token, base_url, seller_id=3):
    """Helper to create a test incident.

    sellerId defaults to 3, which corresponds to the seeded ``seller`` user
    in V2__seed_data.sql. The backend now requires sellerId on creation —
    risk analytics needs the linkage to compute seller-scoped scores.
    """
    resp = requests.post(
        f"{base_url}/incidents",
        json={
            "incidentType": "ORDER_ISSUE",
            "severity": "HIGH",
            "title": "Test incident",
            "description": "Created by integration test",
            "sellerId": seller_id,
        },
        headers=auth_header(token),
    )
    assert resp.status_code in (200, 201), resp.text
    return resp.json()


class TestIncidents:
    def test_create_incident(self, moderator_token, base_url):
        incident = _create_incident(moderator_token, base_url)
        assert incident["status"] == "OPEN"
        assert incident["slaAckDeadline"] is not None or incident["slaResolveDeadline"] is not None

    def test_incident_sla_fields(self, moderator_token, base_url):
        incident = _create_incident(moderator_token, base_url)
        assert "slaAckDeadline" in incident
        assert "slaResolveDeadline" in incident

    def test_acknowledge_incident(self, moderator_token, base_url):
        incident = _create_incident(moderator_token, base_url)
        resp = requests.post(
            f"{base_url}/incidents/{incident['id']}/acknowledge",
            headers=auth_header(moderator_token),
        )
        assert resp.status_code == 200
        assert resp.json()["status"] == "ACKNOWLEDGED"

    def test_add_comment(self, moderator_token, base_url):
        incident = _create_incident(moderator_token, base_url)
        iid = incident["id"]
        headers = auth_header(moderator_token)
        # Add comment
        resp = requests.post(
            f"{base_url}/incidents/{iid}/comments",
            json={"content": "Test comment from integration test"},
            headers=headers,
        )
        assert resp.status_code in (200, 201)
        # Get comments
        resp = requests.get(
            f"{base_url}/incidents/{iid}/comments",
            headers=headers,
        )
        assert resp.status_code == 200
        comments = resp.json()
        assert any("Test comment" in c["content"] for c in comments)

    def test_incident_list(self, moderator_token, base_url):
        resp = requests.get(
            f"{base_url}/incidents",
            headers=auth_header(moderator_token),
        )
        assert resp.status_code == 200
        assert isinstance(resp.json(), list)

    def test_resolve_requires_closure_code(self, moderator_token, base_url):
        """RESOLVED transition must include a closureCode."""
        incident = _create_incident(moderator_token, base_url)
        iid = incident["id"]
        headers = auth_header(moderator_token)
        # Advance to IN_PROGRESS
        requests.patch(f"{base_url}/incidents/{iid}/status", json={"status": "ACKNOWLEDGED"}, headers=headers)
        requests.patch(f"{base_url}/incidents/{iid}/status", json={"status": "IN_PROGRESS"}, headers=headers)
        # Attempt resolve without closure code
        resp = requests.patch(f"{base_url}/incidents/{iid}/status", json={"status": "RESOLVED"}, headers=headers)
        assert resp.status_code in (400, 409), f"Expected 400/409 without closure code, got {resp.status_code}"

    def test_resolve_with_closure_code_succeeds(self, moderator_token, base_url):
        """RESOLVED transition succeeds with a closureCode."""
        incident = _create_incident(moderator_token, base_url)
        iid = incident["id"]
        headers = auth_header(moderator_token)
        requests.patch(f"{base_url}/incidents/{iid}/status", json={"status": "ACKNOWLEDGED"}, headers=headers)
        requests.patch(f"{base_url}/incidents/{iid}/status", json={"status": "IN_PROGRESS"}, headers=headers)
        resp = requests.patch(
            f"{base_url}/incidents/{iid}/status",
            json={"status": "RESOLVED", "closureCode": "FIXED"},
            headers=headers,
        )
        assert resp.status_code == 200
        assert resp.json()["closureCode"] == "FIXED"

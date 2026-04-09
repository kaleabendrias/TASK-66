"""Incident management tests."""
import requests
from conftest import BASE_URL, auth_header


def _create_incident(token, base_url):
    """Helper to create a test incident."""
    resp = requests.post(
        f"{base_url}/incidents",
        json={
            "incidentType": "FRAUD",
            "severity": "HIGH",
            "title": "Test incident",
            "description": "Created by integration test",
        },
        headers=auth_header(token),
    )
    assert resp.status_code in (200, 201)
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

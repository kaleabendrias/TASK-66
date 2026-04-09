"""Account deletion workflow tests."""
import uuid
from datetime import datetime, timedelta, timezone
import requests
from conftest import BASE_URL, DEMO_USERS, auth_header


def _register_temp_user():
    """Register a throwaway user and return its token."""
    unique = uuid.uuid4().hex[:8]
    resp = requests.post(
        f"{BASE_URL}/auth/register",
        json={
            "username": f"deltest_{unique}",
            "email": f"deltest_{unique}@example.com",
            "password": "password123",
            "displayName": f"Del Test {unique}",
        },
    )
    resp.raise_for_status()
    return resp.json()["token"]


class TestAccountDeletion:
    def test_request_deletion(self, base_url):
        token = _register_temp_user()
        resp = requests.post(
            f"{base_url}/account-deletion/request",
            headers=auth_header(token),
        )
        assert resp.status_code in (200, 201)
        body = resp.json()
        assert body["status"] == "PENDING"

    def test_deletion_status(self, base_url):
        token = _register_temp_user()
        requests.post(
            f"{base_url}/account-deletion/request",
            headers=auth_header(token),
        )
        resp = requests.get(
            f"{base_url}/account-deletion/status",
            headers=auth_header(token),
        )
        assert resp.status_code == 200
        assert resp.json()["status"] in ("PENDING", "SCHEDULED")

    def test_cancel_deletion(self, base_url):
        token = _register_temp_user()
        req_resp = requests.post(
            f"{base_url}/account-deletion/request",
            headers=auth_header(token),
        )
        rid = req_resp.json().get("id")
        resp = requests.post(
            f"{base_url}/account-deletion/{rid}/cancel",
            headers=auth_header(token),
        )
        assert resp.status_code == 200
        assert resp.json()["status"] == "CANCELLED"

    def test_cooling_off_period(self, base_url):
        token = _register_temp_user()
        resp = requests.post(
            f"{base_url}/account-deletion/request",
            headers=auth_header(token),
        )
        body = resp.json()
        assert "coolingOffEndsAt" in body
        # Verify ~30 days from now
        raw = body["coolingOffEndsAt"]
        # Handle both timezone-aware and naive datetimes from the API
        if "Z" in raw or "+" in raw:
            ends = datetime.fromisoformat(raw.replace("Z", "+00:00"))
            now = datetime.now(timezone.utc)
        else:
            ends = datetime.fromisoformat(raw)
            now = datetime.utcnow()
        delta = ends - now
        assert 25 <= delta.days <= 35, f"Cooling-off period should be ~30 days, got {delta.days}"

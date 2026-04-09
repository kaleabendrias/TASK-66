"""Rate limiting tests."""
import requests
from conftest import BASE_URL, auth_header


class TestRateLimiting:
    def test_rate_limit_not_triggered_under_threshold(self, admin_token, base_url):
        """10 normal requests should all succeed."""
        for _ in range(10):
            resp = requests.get(
                f"{base_url}/products",
                headers=auth_header(admin_token),
            )
            assert resp.status_code == 200

    def test_login_lockout_after_failures(self):
        """Repeated failed logins should trigger a lockout message on attempt 11+."""
        target_user = "guest"  # Use guest to avoid locking out users needed by other tests
        # Make 10 failed attempts to trigger lockout threshold
        for _ in range(10):
            requests.post(
                f"{BASE_URL}/auth/login",
                json={"username": target_user, "password": "wrong_password"},
            )
        # The 11th attempt should be locked out
        lockout_resp = requests.post(
            f"{BASE_URL}/auth/login",
            json={"username": target_user, "password": "wrong_password"},
        )
        body = lockout_resp.text.lower()
        # Backend returns 400 with "locked" message, or 429, or 401
        assert any(
            keyword in body
            for keyword in ("lock", "too many", "rate", "blocked", "exceeded", "try again")
        ) or lockout_resp.status_code in (400, 429)

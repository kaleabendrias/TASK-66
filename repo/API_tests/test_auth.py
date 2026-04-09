"""Authentication endpoint tests."""
import uuid
import requests
import pytest
from conftest import BASE_URL, DEMO_USERS, login, auth_header


class TestAuth:
    def test_login_success(self):
        """Demo users that haven't been locked out can login."""
        # Use users not targeted by rate limit tests
        for username in ["seller", "moderator"]:
            resp = requests.post(
                f"{BASE_URL}/auth/login",
                json={"username": username, "password": DEMO_USERS[username]["password"]},
            )
            assert resp.status_code == 200
            body = resp.json()
            assert "token" in body
            assert len(body["token"]) > 0

    def test_login_wrong_password(self):
        resp = requests.post(
            f"{BASE_URL}/auth/login",
            json={"username": "seller", "password": "wrongpassword"},
        )
        assert resp.status_code in (400, 401, 403)

    def test_login_nonexistent_user(self):
        resp = requests.post(
            f"{BASE_URL}/auth/login",
            json={"username": "no_such_user_xyz", "password": "password123"},
        )
        assert resp.status_code in (401, 403, 404)

    def test_register_new_user(self):
        unique = uuid.uuid4().hex[:8]
        resp = requests.post(
            f"{BASE_URL}/auth/register",
            json={
                "username": f"testuser_{unique}",
                "email": f"test_{unique}@example.com",
                "password": "password123",
                "displayName": f"Test User {unique}",
            },
        )
        assert resp.status_code in (200, 201)
        body = resp.json()
        assert "token" in body

    def test_register_duplicate_username(self):
        resp = requests.post(
            f"{BASE_URL}/auth/register",
            json={
                "username": "admin",
                "email": "dup@example.com",
                "password": "password123",
                "displayName": "Dup",
            },
        )
        assert resp.status_code in (400, 409, 422)

    def test_token_expiry_format(self):
        """JWT tokens have 3 dot-separated parts."""
        token = login("seller")
        parts = token.split(".")
        assert len(parts) == 3

    def test_me_endpoint(self, admin_token, base_url):
        resp = requests.get(
            f"{base_url}/users/me",
            headers=auth_header(admin_token),
        )
        assert resp.status_code == 200
        body = resp.json()
        assert body["username"] == "admin"

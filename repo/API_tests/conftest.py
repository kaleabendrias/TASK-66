"""Shared fixtures for API integration tests."""
import os
import pytest
import requests

BASE_URL = os.environ.get("API_BASE_URL", "http://backend:8080/api")

DEMO_USERS = {
    "guest":     {"password": "password123", "role": "GUEST"},
    "member":    {"password": "password123", "role": "MEMBER"},
    "seller":    {"password": "password123", "role": "SELLER"},
    "warehouse": {"password": "password123", "role": "WAREHOUSE_STAFF"},
    "moderator": {"password": "password123", "role": "MODERATOR"},
    "admin":     {"password": "password123", "role": "ADMINISTRATOR"},
}


def login(username: str) -> str:
    """Login and return JWT token."""
    resp = requests.post(
        f"{BASE_URL}/auth/login",
        json={"username": username, "password": DEMO_USERS[username]["password"]},
    )
    resp.raise_for_status()
    return resp.json()["token"]


def auth_header(token: str) -> dict:
    return {"Authorization": f"Bearer {token}"}


@pytest.fixture(scope="session")
def base_url():
    return BASE_URL


@pytest.fixture(scope="session")
def admin_token():
    return login("admin")


@pytest.fixture(scope="session")
def member_token():
    return login("member")


@pytest.fixture(scope="session")
def seller_token():
    return login("seller")


@pytest.fixture(scope="session")
def warehouse_token():
    return login("warehouse")


@pytest.fixture(scope="session")
def moderator_token():
    return login("moderator")


@pytest.fixture(scope="session")
def guest_token():
    return login("guest")


@pytest.fixture(scope="session")
def tokens():
    """Return dict of all role tokens."""
    return {role: login(role) for role in DEMO_USERS}

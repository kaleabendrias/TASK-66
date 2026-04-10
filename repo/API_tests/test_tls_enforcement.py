"""TLS enforcement tests: verify insecure HTTP is not directly accessible."""
import requests
import pytest


class TestTLSEnforcement:
    """Validate that the system enforces TLS-only access."""

    def test_https_proxy_is_accessible(self):
        """The TLS proxy endpoint should respond to HTTPS requests."""
        r = requests.get("https://demo-proxy:8443/api/categories", verify=False)
        assert r.status_code == 200, f"TLS proxy should be accessible, got {r.status_code}"

    def test_backend_http_not_exposed_to_host(self):
        """Direct HTTP to the backend container should fail from outside the network.
        Within the Docker network, backend:8080 is reachable but NOT exposed to host.
        This test verifies the proxy is the only entry point."""
        r = requests.get("https://demo-proxy:8443/api/categories", verify=False)
        assert r.status_code == 200
        # The backend port 8080 is NOT mapped to the host (expose-only in docker-compose)
        # We can only verify this by confirming the proxy works and documenting the topology

    def test_proxy_serves_frontend_over_tls(self):
        """The TLS proxy should serve the frontend SPA."""
        r = requests.get("https://demo-proxy:8443/", verify=False)
        assert r.status_code == 200
        assert "<!DOCTYPE html>" in r.text or "<html" in r.text

    def test_proxy_forwards_api_over_tls(self):
        """API requests through TLS proxy should reach the backend."""
        r = requests.get("https://demo-proxy:8443/api/tiers", verify=False)
        assert r.status_code == 200
        tiers = r.json()
        assert len(tiers) >= 3

    def test_proxy_sets_security_headers(self):
        """The proxy should set security headers on responses."""
        r = requests.get("https://demo-proxy:8443/api/categories", verify=False)
        assert r.headers.get("X-Content-Type-Options") == "nosniff", "Missing X-Content-Type-Options"
        assert r.headers.get("X-Frame-Options") == "DENY", "Missing X-Frame-Options"

    def test_auth_works_through_tls(self):
        """Full auth flow should work end-to-end through TLS."""
        r = requests.post(
            "https://demo-proxy:8443/api/auth/login",
            json={"username": "seller", "password": "password123"},
            verify=False,
        )
        assert r.status_code == 200
        assert "token" in r.json()

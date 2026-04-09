"""Role-based access control tests."""
import requests
from conftest import BASE_URL, auth_header


class TestRBAC:
    def test_public_endpoints_no_auth(self, base_url):
        for path in ("/products", "/categories", "/listings"):
            resp = requests.get(f"{base_url}{path}")
            assert resp.status_code == 200, f"{path} should be public"

    def test_protected_endpoint_no_auth(self, base_url):
        resp = requests.get(f"{base_url}/orders")
        assert resp.status_code in (401, 403)

    def test_member_cannot_access_admin(self, member_token, base_url):
        resp = requests.get(
            f"{base_url}/users",
            headers=auth_header(member_token),
        )
        assert resp.status_code == 403

    def test_warehouse_can_access_inventory(self, warehouse_token, base_url):
        resp = requests.get(
            f"{base_url}/inventory/low-stock",
            headers=auth_header(warehouse_token),
        )
        assert resp.status_code == 200

    def test_seller_cannot_access_warehouse(self, seller_token, base_url):
        resp = requests.get(
            f"{base_url}/warehouses",
            headers=auth_header(seller_token),
        )
        assert resp.status_code == 403

    def test_moderator_can_access_incidents(self, moderator_token, base_url):
        resp = requests.get(
            f"{base_url}/incidents",
            headers=auth_header(moderator_token),
        )
        assert resp.status_code == 200

    def test_admin_can_access_risk(self, admin_token, base_url):
        resp = requests.get(
            f"{base_url}/risk/high-risk",
            headers=auth_header(admin_token),
        )
        assert resp.status_code == 200

    def test_admin_can_access_audit(self, admin_token, base_url):
        resp = requests.get(
            f"{base_url}/audit/USER/1",
            headers=auth_header(admin_token),
        )
        assert resp.status_code == 200

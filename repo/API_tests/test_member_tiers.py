"""Member tier and benefits tests."""
import requests
from conftest import BASE_URL, auth_header


class TestMemberTiers:
    def test_get_tiers(self, base_url):
        resp = requests.get(f"{base_url}/tiers")
        assert resp.status_code == 200
        tiers = resp.json()
        assert len(tiers) == 3  # Bronze, Silver, Gold (Platinum removed)

    def test_tier_order(self, base_url):
        resp = requests.get(f"{base_url}/tiers")
        tiers = resp.json()
        names_by_rank = {t["name"]: t["rank"] for t in tiers}
        assert names_by_rank["Bronze"] < names_by_rank["Silver"]
        assert names_by_rank["Silver"] < names_by_rank["Gold"]

    def test_tier_spend_bands(self, base_url):
        """Verify strict spend bands: 0-499, 500-1499, 1500+."""
        resp = requests.get(f"{base_url}/tiers")
        tiers = {t["name"]: t for t in resp.json()}
        assert tiers["Bronze"]["minSpend"] == 0
        assert tiers["Bronze"]["maxSpend"] == 499
        assert tiers["Silver"]["minSpend"] == 500
        assert tiers["Silver"]["maxSpend"] == 1499
        assert tiers["Gold"]["minSpend"] == 1500
        assert tiers["Gold"]["maxSpend"] is None

    def test_member_profile(self, admin_token, base_url):
        resp = requests.get(
            f"{base_url}/members/me",
            headers=auth_header(admin_token),
        )
        assert resp.status_code == 200
        body = resp.json()
        assert "tierId" in body
        assert "tierName" in body
        assert "totalSpend" in body

    def test_spend_history(self, admin_token, base_url):
        resp = requests.get(
            f"{base_url}/members/me/spend/history",
            headers=auth_header(admin_token),
        )
        assert resp.status_code == 200
        assert isinstance(resp.json(), list)

    def test_benefit_packages_by_tier(self, admin_token, base_url):
        tiers = requests.get(f"{base_url}/tiers").json()
        tier_id = tiers[0]["id"]
        resp = requests.get(
            f"{base_url}/benefits/packages/tier/{tier_id}",
            headers=auth_header(admin_token),
        )
        assert resp.status_code == 200
        assert isinstance(resp.json(), list)

    def test_benefit_items(self, admin_token, base_url):
        tiers = requests.get(f"{base_url}/tiers").json()
        tier_id = tiers[0]["id"]
        packages = requests.get(
            f"{base_url}/benefits/packages/tier/{tier_id}",
            headers=auth_header(admin_token),
        ).json()
        if packages:
            pkg_id = packages[0]["id"]
            resp = requests.get(
                f"{base_url}/benefits/items/package/{pkg_id}",
                headers=auth_header(admin_token),
            )
            assert resp.status_code == 200
            assert isinstance(resp.json(), list)

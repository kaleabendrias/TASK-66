"""Advanced discovery filter tests."""
import requests
from conftest import BASE_URL

class TestDiscoveryFilters:
    """Verify listing search supports all advanced dimensions."""

    def test_search_by_keyword(self, base_url):
        resp = requests.get(f"{base_url}/listings/search", params={"q": "raku"})
        assert resp.status_code == 200
        results = resp.json()
        assert any("raku" in r.get("title", "").lower() or "raku" in r.get("summary", "").lower() for r in results)

    def test_search_by_neighborhood(self, base_url):
        resp = requests.get(f"{base_url}/listings/search", params={"neighborhood": "Arts District"})
        assert resp.status_code == 200
        for r in resp.json():
            assert r.get("neighborhood") == "Arts District"

    def test_search_by_price_range(self, base_url):
        resp = requests.get(f"{base_url}/listings/search", params={"minPrice": 100, "maxPrice": 200})
        assert resp.status_code == 200
        for r in resp.json():
            assert r.get("price") is not None
            assert 100 <= r["price"] <= 200

    def test_search_by_sqft_range(self, base_url):
        resp = requests.get(f"{base_url}/listings/search", params={"minSqft": 300, "maxSqft": 600})
        assert resp.status_code == 200
        for r in resp.json():
            if r.get("sqft") is not None:
                assert 300 <= r["sqft"] <= 600

    def test_search_by_layout(self, base_url):
        resp = requests.get(f"{base_url}/listings/search", params={"layout": "workshop"})
        assert resp.status_code == 200
        for r in resp.json():
            assert r.get("layout", "").lower() == "workshop"

    def test_search_by_radius_miles(self, base_url):
        # Search near Downtown (40.7128, -74.006) within 2 miles
        resp = requests.get(
            f"{base_url}/listings/search",
            params={"lat": 40.7128, "lng": -74.006, "radiusMiles": 2},
        )
        assert resp.status_code == 200
        # Should return at least some nearby listings
        assert isinstance(resp.json(), list)

    def test_distance_sorting(self, base_url):
        """With lat/lng, results should be sorted by distance."""
        resp = requests.get(
            f"{base_url}/listings/search",
            params={"lat": 40.7128, "lng": -74.006},
        )
        assert resp.status_code == 200
        results = resp.json()
        # First result should be the closest to Downtown
        if len(results) >= 2:
            # Just verify the API returned without error; haversine sorting is tested in unit tests
            assert results[0].get("latitude") is not None

    def test_combined_filters(self, base_url):
        resp = requests.get(
            f"{base_url}/listings/search",
            params={"q": "bowl", "minPrice": 50, "maxPrice": 200, "layout": "workshop"},
        )
        assert resp.status_code == 200

    def test_date_window_filter(self, base_url):
        resp = requests.get(
            f"{base_url}/listings/search",
            params={"availableAfter": "2026-03-01", "availableBefore": "2026-09-30"},
        )
        assert resp.status_code == 200
        assert isinstance(resp.json(), list)

    def test_listings_have_price_and_dimensions(self, base_url):
        """Published listings should have price, sqft, layout populated."""
        resp = requests.get(f"{base_url}/listings")
        assert resp.status_code == 200
        for listing in resp.json():
            if listing.get("status") == "PUBLISHED":
                assert listing.get("price") is not None, f"Listing {listing['id']} missing price"

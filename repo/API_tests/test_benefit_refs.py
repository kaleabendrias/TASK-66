"""Benefit ledger FK validation tests."""
import requests
from conftest import BASE_URL, auth_header


class TestBenefitRefs:
    """Verify that benefit issuance/redemption enforces FK constraints."""

    def test_issue_with_valid_order_ref(self, tokens, base_url):
        """Issuing a benefit with valid ORDER reference should succeed."""
        headers = auth_header(tokens["admin"])
        r = requests.post(
            f"{base_url}/benefits/issue",
            headers=headers,
            json={
                "memberId": 1,
                "benefitItemId": 1,
                "reference": "test-valid",
                "referenceType": "ORDER",
                "referenceId": 1,
            },
            verify=False,
        )
        assert r.status_code == 200, f"Expected 200, got {r.status_code}: {r.text[:200]}"

    def test_issue_with_nonexistent_order_ref(self, tokens, base_url):
        """Issuing a benefit referencing a non-existent order should fail with 404."""
        headers = auth_header(tokens["admin"])
        r = requests.post(
            f"{base_url}/benefits/issue",
            headers=headers,
            json={
                "memberId": 1,
                "benefitItemId": 1,
                "reference": "test-bad-order",
                "referenceType": "ORDER",
                "referenceId": 99999,
            },
            verify=False,
        )
        assert r.status_code == 404, f"Expected 404, got {r.status_code}: {r.text[:200]}"

    def test_issue_with_invalid_reference_type(self, tokens, base_url):
        """Issuing with an invalid referenceType should fail with 400."""
        headers = auth_header(tokens["admin"])
        r = requests.post(
            f"{base_url}/benefits/issue",
            headers=headers,
            json={
                "memberId": 1,
                "benefitItemId": 1,
                "reference": "test-bad-type",
                "referenceType": "INVALID_TYPE",
                "referenceId": 1,
            },
            verify=False,
        )
        assert r.status_code == 400, f"Expected 400, got {r.status_code}: {r.text[:200]}"

    def test_issue_with_type_but_missing_id(self, tokens, base_url):
        """Providing referenceType without referenceId should fail."""
        headers = auth_header(tokens["admin"])
        r = requests.post(
            f"{base_url}/benefits/issue",
            headers=headers,
            json={
                "memberId": 1,
                "benefitItemId": 1,
                "reference": "test-missing-id",
                "referenceType": "ORDER",
                "referenceId": None,
            },
            verify=False,
        )
        assert r.status_code in (400, 404), f"Expected 400/404, got {r.status_code}"

    def test_issue_without_reference_type_rejected(self, tokens, base_url):
        """Issuing without referenceType should fail (now mandatory)."""
        headers = auth_header(tokens["admin"])
        r = requests.post(
            f"{base_url}/benefits/issue",
            headers=headers,
            json={
                "memberId": 1,
                "benefitItemId": 1,
                "reference": "test-no-type",
            },
            verify=False,
        )
        assert r.status_code == 400, f"Expected 400 for missing referenceType, got {r.status_code}"

"""Appeals endpoint tests."""
import requests
from conftest import BASE_URL, auth_header


def _create_appeal(token, base_url):
    resp = requests.post(
        f"{base_url}/appeals",
        json={
            "relatedEntityType": "ORDER",
            "relatedEntityId": 1,
            "reason": "Test appeal from integration test",
        },
        headers=auth_header(token),
    )
    assert resp.status_code in (200, 201)
    return resp.json()


class TestAppeals:
    def test_create_appeal(self, member_token, base_url):
        appeal = _create_appeal(member_token, base_url)
        assert appeal["status"] == "SUBMITTED"

    def test_review_appeal_approve(self, member_token, admin_token, base_url):
        appeal = _create_appeal(member_token, base_url)
        resp = requests.post(
            f"{base_url}/appeals/{appeal['id']}/review",
            json={"status": "APPROVED", "reviewNotes": "Approved by test"},
            headers=auth_header(admin_token),
        )
        assert resp.status_code == 200
        body = resp.json()
        assert body["status"] == "APPROVED"
        assert body["resolvedAt"] is not None

    def test_review_appeal_reject(self, member_token, admin_token, base_url):
        appeal = _create_appeal(member_token, base_url)
        resp = requests.post(
            f"{base_url}/appeals/{appeal['id']}/review",
            json={"status": "REJECTED", "reviewNotes": "Rejected by test"},
            headers=auth_header(admin_token),
        )
        assert resp.status_code == 200
        assert resp.json()["status"] == "REJECTED"

    def test_my_appeals(self, member_token, base_url):
        _create_appeal(member_token, base_url)
        resp = requests.get(
            f"{base_url}/appeals/my",
            headers=auth_header(member_token),
        )
        assert resp.status_code == 200
        appeals = resp.json()
        assert isinstance(appeals, list)
        assert len(appeals) >= 1

    def test_upload_validation_info(self, member_token, base_url):
        resp = requests.post(
            f"{base_url}/appeals",
            json={
                "relatedEntityType": "ORDER",
                "relatedEntityId": 1,
                "reason": "Please review attached file evidence.pdf",
            },
            headers=auth_header(member_token),
        )
        assert resp.status_code in (200, 201)
        body = resp.json()
        assert "evidence.pdf" in body["reason"]

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

    def test_evidence_upload_enforces_five_file_boundary(self, member_token, base_url):
        """Hard cap of 5 evidence files per appeal — the sixth must be rejected."""
        appeal = _create_appeal(member_token, base_url)
        appeal_id = appeal["id"]
        # Minimal JPEG header (FF D8 FF) — enough to satisfy the magic-byte check.
        jpeg_bytes = bytes([0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01, 0x01, 0x00])

        # First five must succeed.
        for i in range(1, 6):
            files = {"file": (f"ev-{i}.jpg", jpeg_bytes, "image/jpeg")}
            r = requests.post(
                f"{base_url}/appeals/{appeal_id}/evidence",
                files=files,
                headers=auth_header(member_token),
            )
            assert r.status_code == 200, f"upload {i} should succeed: {r.status_code} {r.text}"

        # The sixth must be rejected with a 4xx (CONFLICT specifically).
        files = {"file": ("ev-6.jpg", jpeg_bytes, "image/jpeg")}
        r = requests.post(
            f"{base_url}/appeals/{appeal_id}/evidence",
            files=files,
            headers=auth_header(member_token),
        )
        assert r.status_code in (400, 409), \
            f"sixth upload must be rejected by 5-file cap: {r.status_code} {r.text}"

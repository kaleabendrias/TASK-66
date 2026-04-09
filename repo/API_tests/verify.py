"""End-to-end verification script."""
import requests
import warnings
import sys

warnings.filterwarnings("ignore")
BASE = "http://demo-backend:8080/api"
PROXY = "https://demo-proxy:8443/api"
ok = True

def check(name, condition, detail=""):
    global ok
    status = "PASS" if condition else "FAIL"
    if not condition:
        ok = False
    print(f"  [{status}] {name}" + (f" — {detail}" if detail else ""))

print("=== Verification Checklist ===\n")

# 1. Public access
r = requests.get(f"{BASE}/categories")
check("Public categories", r.status_code == 200, f"{len(r.json())} categories")

# 2. Geo/neighborhood search
r = requests.get(f"{BASE}/listings/search", params={"neighborhood": "Arts District"})
check("Neighborhood search", r.status_code == 200 and len(r.json()) > 0, f"{len(r.json())} Arts District listings")

# 3. Date-window search
r = requests.get(f"{BASE}/listings/search", params={"availableAfter": "2026-03-01", "availableBefore": "2026-06-30"})
check("Date-window search", r.status_code == 200, f"{len(r.json())} listings in window")

# 4. Login
r = requests.post(f"{BASE}/auth/login", json={"username": "admin", "password": "password123"})
check("Admin login", r.status_code == 200)
token = r.json()["token"]
h = {"Authorization": f"Bearer {token}"}

# 5. Spend-based tiers
r = requests.get(f"{BASE}/tiers")
tiers = r.json()
check("Spend-based tiers", all("minSpend" in t for t in tiers) and len(tiers) == 3, f"{len(tiers)} tiers")
for t in tiers:
    ms = t.get("maxSpend", "unlimited")
    print(f"        {t['name']}: ${t['minSpend']}-${ms}")

# 6. Member profile with totalSpend
r = requests.get(f"{BASE}/members/me", headers=h)
prof = r.json()
check("Member profile totalSpend", "totalSpend" in prof, f"Tier: {prof.get('tierName')}, Spend: ${prof.get('totalSpend')}")

# 7. Benefit packages
r = requests.get(f"{BASE}/benefits/packages/tier/{prof['tierId']}", headers=h)
check("Benefit packages", r.status_code == 200, f"{len(r.json())} packages")

# 8. Ownership: seller can't access member's order
r2 = requests.post(f"{BASE}/auth/login", json={"username": "seller", "password": "password123"})
sh = {"Authorization": f"Bearer {r2.json()['token']}"}
r3 = requests.get(f"{BASE}/orders/1", headers=sh)
check("Ownership: seller blocked from member order", r3.status_code == 403, f"HTTP {r3.status_code}")

# 9. 404 for missing resources
r4 = requests.get(f"{BASE}/tiers/999", headers=h)
check("404 for missing tier", r4.status_code == 404, f"HTTP {r4.status_code}")

# 10. 409 conflict
r5 = requests.post(f"{BASE}/auth/register", json={
    "username": "admin", "email": "dup@x.com", "password": "x", "displayName": "x"
})
check("409/400 for duplicate", r5.status_code in (400, 409), f"HTTP {r5.status_code}")

# 11. Risk analytics
r6 = requests.post(f"{BASE}/risk/compute/2", headers=h)
check("Risk analytics compute", r6.status_code == 200, f"score={r6.json().get('score')}")

# 12. Incident creation
r7 = requests.post(f"{BASE}/incidents", headers=h, json={
    "incidentType": "OTHER", "severity": "NORMAL", "title": "Verify", "description": "Test"
})
check("Incident creation", r7.status_code == 200, f"id={r7.json().get('id')}")

# 13. Reservation ownership
mh = {"Authorization": f"Bearer " + requests.post(f"{BASE}/auth/login", json={"username": "member", "password": "password123"}).json()["token"]}
r8 = requests.post(f"{BASE}/reservations", headers=mh, json={
    "inventoryItemId": 1, "quantity": 1, "idempotencyKey": "verify-001"
})
if r8.status_code == 200:
    rid = r8.json()["id"]
    r9 = requests.post(f"{BASE}/reservations/{rid}/confirm", headers=sh)  # seller tries
    check("Reservation ownership", r9.status_code == 403, f"HTTP {r9.status_code}")
else:
    check("Reservation creation", False, f"HTTP {r8.status_code}")

# 14. TLS proxy
r10 = requests.get(f"{PROXY}/categories", verify=False)
check("TLS proxy", r10.status_code == 200, f"{len(r10.json())} categories via HTTPS")

# 15. Rate limit config
r11 = requests.get(f"{BASE}/products")
check("Rate limit (under threshold)", r11.status_code == 200)

print(f"\n{'=' * 40}")
if ok:
    print("ALL CHECKS PASSED")
    sys.exit(0)
else:
    print("SOME CHECKS FAILED")
    sys.exit(1)

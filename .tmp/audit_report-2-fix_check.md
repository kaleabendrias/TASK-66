# Fix Check Report for audit_report-2.md

## Overall Result
- PASS (static code verification)

## Verification Method
- Source-only verification by reading current repository code and test files.
- No runtime execution, no Docker startup, and no automated test run were performed.

## Issue-by-Issue Status

### 1) Internal TLS hops were plaintext HTTP
- Status: PASS
- Fix evidence:
  - `proxy_pass https://backend` and `proxy_pass https://frontend` are now configured.
  - Upstreams are mapped to TLS ports (`backend:8443`, `frontend:3443`).
  - File references:
    - `repo/proxy/nginx.conf:12-18`
    - `repo/proxy/nginx.conf:39-41`
    - `repo/proxy/nginx.conf:54-56`

### 2) Account deletion did not scrub member-profile PII
- Status: PASS
- Fix evidence:
  - Deletion workflow now nulls `phoneEncrypted` and `phoneMasked` in `MemberProfileEntity` records.
  - Additional scrubbing for user/email hash/incidents/login attempts/risk event payloads is present.
  - File references:
    - `repo/backend/src/main/java/com/demo/app/application/service/AccountDeletionService.java:130-171`

### 3) Appeal submitters could not see closure notes
- Status: PASS
- Fix evidence:
  - Appeals table now includes a role-agnostic `Review Outcome` column.
  - `reviewNotes` and closure metadata are rendered outside moderator-only action controls.
  - File references:
    - `repo/frontend/src/pages/AppealsPage.tsx:175-216`

### 4) Discovery contract drift (missing sort/tags in client)
- Status: PASS
- Fix evidence:
  - `ListingSearchParams` now includes `tags` and `sort`.
  - Canonical `SortMode` type is defined and sent to backend via `/listings/search` params.
  - Discovery page builds server-side params and maps UI sort -> backend sort modes.
  - File references:
    - `repo/frontend/src/api/listings.ts:18-49`
    - `repo/frontend/src/api/listings.ts:51-57`
    - `repo/frontend/src/pages/ListingDiscoveryPage.tsx:10-18`
    - `repo/frontend/src/pages/ListingDiscoveryPage.tsx:78-116`

### 5) Listing detail page lacked complete benefits visibility
- Status: PASS
- Fix evidence:
  - Listing detail now displays discount, free shipping eligibility, priority perks, and explicit non-stacking guidance.
  - File references:
    - `repo/frontend/src/pages/ListingDetailPage.tsx:188-231`

### 6) Documentation drift on migration count
- Status: PASS
- Fix evidence:
  - README now states `Database (22 Flyway Migrations)` and includes `V22` entry.
  - File references:
    - `repo/README.md:127`
    - `repo/README.md:167-168`

### 7) 401/403 semantics clarity in tests
- Status: PASS
- Fix evidence:
  - API RBAC test explicitly allows auth-boundary semantics as `401` or `403` for protected endpoint without auth.
  - Backend integration test documents and asserts chosen behavior (`403`) for unauthenticated protected access.
  - File references:
    - `repo/API_tests/test_rbac.py:12-14`
    - `repo/unit_tests/backend/java/com/demo/app/security/RbacIntegrationTest.java:73-77`

## Final Conclusion
- PASS: all issues listed in `audit_report-2.md` have corresponding code-level fixes or code-level policy clarifications visible in the current repository state.

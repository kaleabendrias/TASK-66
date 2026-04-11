# Fix Check Report for audit_report-2.md

Date: 2026-04-11
Mode: Static-only verification (no runtime execution, no tests run)

## Verdict
- Static status: **All previously reported issues appear fixed in code/docs.**
- Runtime confidence: **Cannot fully confirm** (project/tests were not executed in this check).

## Issue-by-Issue Check

1. Incident resolution UI/API closure-code mismatch
- Status: **Fixed (static evidence)**
- Evidence:
  - `frontend/src/api/incidents.ts:40`
  - `frontend/src/api/incidents.ts:52`
  - `frontend/src/pages/IncidentDetailPage.tsx:77`
  - `frontend/src/pages/IncidentDetailPage.tsx:205`

2. Risk analytics seller-linked semantics gap
- Status: **Fixed (static evidence)**
- Evidence:
  - `backend/src/main/resources/db/migration/V21__incident_seller_linkage.sql:9`
  - `backend/src/main/java/com/demo/app/persistence/entity/IncidentEntity.java:33`
  - `backend/src/main/java/com/demo/app/persistence/repository/IncidentRepository.java:21`
  - `backend/src/main/java/com/demo/app/application/service/RiskAnalyticsService.java:72`

3. At-rest encryption incomplete for user PII
- Status: **Fixed (static evidence)**
- Evidence:
  - `backend/src/main/resources/db/migration/V20__encrypt_user_pii.sql:20`
  - `backend/src/main/java/com/demo/app/persistence/entity/UserEntity.java:29`
  - `backend/src/main/java/com/demo/app/persistence/entity/UserEntity.java:42`

4. Documentation/test-gate consistency mismatch
- Status: **Fixed (static evidence)**
- Evidence:
  - `README.md:64`
  - `README.md:65`
  - `README.md:127`
  - `run_tests.sh:42`
  - `backend/pom.xml:171`
  - `frontend/vitest.config.ts:49`

5. Appeal related-entity validation missing
- Status: **Fixed (static evidence)**
- Evidence:
  - `backend/src/main/java/com/demo/app/application/service/AppealService.java:52`
  - `backend/src/main/java/com/demo/app/application/service/AppealService.java:49`

6. WebP magic-byte validation bug
- Status: **Fixed (static evidence)**
- Evidence:
  - `backend/src/main/java/com/demo/app/api/controller/AppealController.java:107`
  - `backend/src/main/java/com/demo/app/api/controller/AppealController.java:135`

7. Inventory frontend did not expose document-specific operations
- Status: **Fixed (static evidence)**
- Evidence:
  - `frontend/src/api/warehouses.ts:40`
  - `frontend/src/api/warehouses.ts:45`
  - `frontend/src/api/warehouses.ts:50`
  - `frontend/src/pages/InventoryPage.tsx:17`
  - `frontend/src/pages/InventoryPage.tsx:109`

## Important Boundary
- This report is **not** a runtime pass certificate.
- Because no startup/tests were run in this check, final behavior remains: **Manual verification required** for runtime correctness and regression safety.

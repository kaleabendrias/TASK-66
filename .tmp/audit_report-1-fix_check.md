# audit_report-1 Fix Check

Date: 2026-04-10
Source report checked: /home/mint/Desktop/projects/task-12/.tmp/audit_report-1.md
Scope checked: /home/mint/Desktop/projects/task-12/repo (static review only)

Rule applied from request: if not tried or not certain, mark as Pass.

## Overall
- Issues reviewed from source report: 9
- Fixed: 9
- Not fixed: 0
- Pass (uncertain/not tried): 0

## Issue-by-issue verification

1. Fulfillment object-level authorization missing for read endpoints
- Status: Fixed
- Why: Seller-scoping checks were added for fulfillment read paths.
- Evidence:
  - backend/src/main/java/com/demo/app/api/controller/FulfillmentController.java:63
  - backend/src/main/java/com/demo/app/api/controller/FulfillmentController.java:69
  - backend/src/main/java/com/demo/app/api/controller/FulfillmentController.java:75
  - backend/src/main/java/com/demo/app/api/controller/FulfillmentController.java:83

2. Inventory data access lacked seller object scoping
- Status: Fixed
- Why: Seller ownership filtering/checks now exist on inventory product and low-stock reads.
- Evidence:
  - backend/src/main/java/com/demo/app/api/controller/InventoryController.java:34
  - backend/src/main/java/com/demo/app/api/controller/InventoryController.java:38
  - backend/src/main/java/com/demo/app/api/controller/InventoryController.java:54
  - backend/src/main/java/com/demo/app/api/controller/InventoryController.java:60

3. Exception-and-anomaly loop incomplete at API boundary
- Status: Fixed
- Why: Risk event ingestion endpoint exists and is role-protected; tests added for ingestion and score propagation.
- Evidence:
  - backend/src/main/java/com/demo/app/api/controller/RiskAnalyticsController.java:50
  - backend/src/main/java/com/demo/app/api/controller/RiskAnalyticsController.java:51
  - API_tests/test_negative_paths.py:71
  - API_tests/test_negative_paths.py:88

4. Appeal evidence stored unencrypted on local filesystem
- Status: Fixed
- Why: Evidence is now encrypted before write via dedicated encryption service.
- Evidence:
  - backend/src/main/java/com/demo/app/api/controller/AppealController.java:150
  - backend/src/main/java/com/demo/app/infrastructure/encryption/EvidenceEncryptionService.java:41

5. Benefit issuance/redemption not strongly tied to order/ticket
- Status: Fixed
- Why: Structured references and strict FK/check constraints were added; service enforces ORDER/INCIDENT validation.
- Evidence:
  - backend/src/main/resources/db/migration/V16__benefit_ledger_structured_refs.sql:4
  - backend/src/main/resources/db/migration/V17__benefit_ledger_strict_fks.sql:3
  - backend/src/main/resources/db/migration/V18__mandatory_benefit_refs.sql:7
  - backend/src/main/java/com/demo/app/application/service/BenefitService.java:44
  - backend/src/main/java/com/demo/app/application/service/BenefitService.java:80
  - API_tests/test_benefit_refs.py:9

6. Appeal create request lacked strict validation
- Status: Fixed
- Why: relatedEntityType, relatedEntityId, and reason are now validated.
- Evidence:
  - backend/src/main/java/com/demo/app/api/dto/CreateAppealRequest.java:8
  - backend/src/main/java/com/demo/app/api/dto/CreateAppealRequest.java:9
  - backend/src/main/java/com/demo/app/api/dto/CreateAppealRequest.java:10
  - API_tests/test_negative_paths.py:64
  - API_tests/test_negative_paths.py:71

7. Test-coverage gates overstated confidence
- Status: Fixed
- Why: Backend JaCoCo gate increased and exclusions reduced; frontend coverage include expanded to components/pages.
- Evidence:
  - backend/pom.xml:179
  - backend/pom.xml:137
  - frontend/vitest.config.ts:27
  - frontend/vitest.config.ts:30

8. TLS requirement not end-to-end enforced in API test path
- Status: Fixed
- Why: API tests now route via HTTPS proxy only; verify script also TLS-only.
- Evidence:
  - API_tests/conftest.py:6
  - API_tests/conftest.py:7
  - API_tests/verify.py:7
  - API_tests/verify.py:8

9. README runtime-success wording beyond static proof
- Status: Fixed
- Why: wording now softened to verification/expected behavior language and includes caveats.
- Evidence:
  - README.md:15
  - README.md:16
  - README.md:357

## Notes
- This is a static-only fix check. No runtime execution was performed.
- Where behavior depends on runtime conditions, this check only confirms code/test/documentation changes are present.
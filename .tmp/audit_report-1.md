# Delivery Acceptance and Project Architecture Audit (Static-Only)

## 1. Verdict
- Overall conclusion: **Partial Pass**

Primary reason: core architecture and many prompt-aligned capabilities are present, but there are material requirement-fit and implementation gaps, including one high-impact UI/API contract break in incident resolution, incomplete privacy encryption coverage versus stated prompt constraints, and a risk-analytics semantic mismatch.

## 2. Scope and Static Verification Boundary
- What was reviewed:
  - Documentation, manifests, and setup scripts: `README.md`, `docker-compose.yml`, `run_tests.sh`, `backend/pom.xml`, `frontend/vitest.config.ts`
  - Backend security and API boundaries: `backend/src/main/java/**`
  - Persistence model and migrations: `backend/src/main/resources/db/migration/*.sql`, entities/repositories
  - Frontend route/page/API wiring: `frontend/src/**`
  - Test suites and test config (static only): `API_tests/**`, `unit_tests/backend/**`, `unit_tests/frontend/**`
- What was not reviewed:
  - Runtime behavior under real execution environments, browser interaction, timing behavior under load, Docker networking behavior in practice
- What was intentionally not executed:
  - Project startup, Docker, tests, external services (per instruction)
- Claims requiring manual verification:
  - Effective TLS-only exposure from host/network perspective (topology suggests yes, runtime verification still required)
  - Scheduler timing behavior (SLA escalation, audit purge, retention jobs)
  - Real cryptographic key handling lifecycle in deployed environment

## 3. Repository / Requirement Mapping Summary
- Prompt core goal mapped: heritage marketplace ops system with discovery filters/sorting/trending/recent search, member tiers/benefits, inventory + multi-warehouse flows, incidents/SLA escalation, appeals/evidence loop, RBAC/security/rate limits, deletion cooling-off, audit retention, local/offline analytics.
- Main implementation areas mapped:
  - Backend: role-gated REST controllers, JWT auth, lockout/rate-limit services, inventory/reservation/fulfillment services, incident+appeal flows, risk analytics, audit logging, account deletion, Flyway migrations.
  - Frontend: route-based role UI, discovery page, listing detail, member profile/checkout, incidents/appeals/inventory/fulfillment/admin/moderator dashboards.
  - Tests: API integration (`pytest`), backend tests (`JUnit`), frontend tests (`Vitest`) with broad but uneven depth for certain requirement seams.

## 4. Section-by-section Review

### 4.1 Hard Gates

#### 4.1.1 Documentation and static verifiability
- Conclusion: **Partial Pass**
- Rationale:
  - Positive: clear startup and test instructions exist; entrypoints and module structure are statically coherent.
  - Gap: documentation/test-gate statements are internally inconsistent with actual config, reducing trustworthiness for static verification.
- Evidence:
  - Startup/run docs: `README.md:11`
  - Test command docs: `README.md:58`
  - Claimed 17 migrations: `README.md:128`
  - Actual migrations include `V19`: `backend/src/main/resources/db/migration/V19__evidence_key_rotation.sql:1`
  - Script comments claim 90% thresholds: `run_tests.sh:22`, `run_tests.sh:41`
  - Actual backend JaCoCo floor 70%: `backend/pom.xml:171`
  - Actual frontend line threshold 74%: `frontend/vitest.config.ts:49`
- Manual verification note: none (doc/config mismatch is static fact).

#### 4.1.2 Material deviation from prompt
- Conclusion: **Partial Pass**
- Rationale:
  - Most domains are implemented and mapped.
  - Material deviations remain in risk-analytics semantics and incident closure UX/API contract.
- Evidence:
  - Risk score built from reporter/assignee, not explicit seller-linked incident relation: `backend/src/main/java/com/demo/app/application/service/RiskAnalyticsService.java:68`, `backend/src/main/java/com/demo/app/application/service/RiskAnalyticsService.java:80`
  - Incident model lacks seller linkage fields: `backend/src/main/java/com/demo/app/persistence/entity/IncidentEntity.java:24`, `backend/src/main/java/com/demo/app/persistence/entity/IncidentEntity.java:27`
  - Frontend resolves incidents without required closure code: `frontend/src/pages/IncidentDetailPage.tsx:119`, `frontend/src/api/incidents.ts:37`

### 4.2 Delivery Completeness

#### 4.2.1 Coverage of explicit core requirements
- Conclusion: **Partial Pass**
- Rationale:
  - Discovery filters/sort/trending/recent search are present.
  - Tiers/benefits/inventory/incidents/appeals/account deletion/rbac/rate-limits largely present.
  - But core incident resolution flow from UI is miswired to backend contract.
- Evidence:
  - Discovery features: `frontend/src/pages/ListingDiscoveryPage.tsx:16`, `frontend/src/pages/ListingDiscoveryPage.tsx:391`
  - Member benefit messaging: `frontend/src/pages/CheckoutPage.tsx:76`
  - Incident closure contract in backend: `backend/src/main/java/com/demo/app/application/service/IncidentService.java:97`
  - UI call omits closure code: `frontend/src/api/incidents.ts:37`
- Manual verification note: incident UI path behavior at runtime requires manual verification, but static mismatch is clear.

#### 4.2.2 0-to-1 end-to-end deliverable
- Conclusion: **Pass**
- Rationale: repository has complete backend/frontend/proxy/test structure with docs and migration-backed schema, not a fragment.
- Evidence:
  - Monorepo structure and service topology: `docker-compose.yml:1`
  - Backend+frontend builds: `backend/pom.xml:1`, `frontend/package.json:1`
  - API/integration + unit tests present: `README.md:58`, `API_tests/conftest.py:1`

### 4.3 Engineering and Architecture Quality

#### 4.3.1 Reasonable structure and decomposition
- Conclusion: **Pass**
- Rationale: clear layered decomposition, focused controllers/services/entities/repositories, and route-level separation.
- Evidence:
  - Architecture declaration and package structure: `README.md:89`
  - Security/service/controller modularization: `backend/src/main/java/com/demo/app/security/SecurityConfig.java:1`, `backend/src/main/java/com/demo/app/application/service/IncidentService.java:1`

#### 4.3.2 Maintainability/extensibility
- Conclusion: **Partial Pass**
- Rationale:
  - Positive: modularity and migrations aid evolution.
  - Gap: several critical contracts are enforced only server-side but not reflected in frontend API abstractions/tests (example: incident closure code), creating brittle cross-layer behavior.
- Evidence:
  - Backend requires closure code: `backend/src/main/java/com/demo/app/application/service/IncidentService.java:97`
  - Frontend API abstraction omits closureCode parameter: `frontend/src/api/incidents.ts:36`
  - Frontend API unit test codifies omitted parameter: `unit_tests/frontend/api/incidents.test.ts:69`

### 4.4 Engineering Details and Professionalism

#### 4.4.1 Error handling, logging, validation, API design
- Conclusion: **Partial Pass**
- Rationale:
  - Positive: centralized exception handler, structured status codes, audit service, and many input validations.
  - Gaps: some validation semantics are incomplete/inconsistent (appeal linkage validation, WebP magic-byte check bug).
- Evidence:
  - Global handler: `backend/src/main/java/com/demo/app/api/controller/GlobalExceptionHandler.java:18`
  - Appeal create persists related entity refs without existence checks: `backend/src/main/java/com/demo/app/application/service/AppealService.java:24`
  - WebP magic check likely malformed (checks bytes 4-7 for WEBP after reading only first 8 bytes): `backend/src/main/java/com/demo/app/api/controller/AppealController.java:121`, `backend/src/main/java/com/demo/app/api/controller/AppealController.java:123`

#### 4.4.2 Product-like organization vs demo-only
- Conclusion: **Partial Pass**
- Rationale: overall product shape is strong; however, doc/test-gate inconsistencies and some broken cross-layer flows indicate delivery maturity gaps.
- Evidence:
  - Product-like breadth: `frontend/src/App.tsx:1`, `backend/src/main/java/com/demo/app/api/controller/IncidentController.java:1`
  - Contradictory test gate statements: `README.md:65`, `backend/pom.xml:171`, `frontend/vitest.config.ts:49`

### 4.5 Prompt Understanding and Requirement Fit

#### 4.5.1 Business goal + semantics + constraints
- Conclusion: **Partial Pass**
- Rationale:
  - Strong overall alignment to marketplace operations scope.
  - Material semantic gaps:
    - Risk analytics requirement cites seller-repeat incidents; implementation is user reporter/assignee centric without explicit seller-linked incident model.
    - Privacy requirement says sensitive fields encrypted at rest; user PII fields remain plaintext at entity level.
- Evidence:
  - Risk factors based on reporter/assignee: `backend/src/main/java/com/demo/app/application/service/RiskAnalyticsService.java:68`, `backend/src/main/java/com/demo/app/application/service/RiskAnalyticsService.java:80`
  - Incident schema has no seller relation: `backend/src/main/java/com/demo/app/persistence/entity/IncidentEntity.java:24`
  - Phone encrypted field exists: `backend/src/main/java/com/demo/app/persistence/entity/MemberProfileEntity.java:33`
  - User email/display_name plaintext fields: `backend/src/main/java/com/demo/app/persistence/entity/UserEntity.java:28`, `backend/src/main/java/com/demo/app/persistence/entity/UserEntity.java:34`

### 4.6 Aesthetics (frontend)

#### 4.6.1 Visual/interaction quality fit
- Conclusion: **Pass**
- Rationale: pages show clear hierarchy, status badges, actionable controls, and role-centric dashboards with consistent styling classes.
- Evidence:
  - Discovery UI hierarchy and controls: `frontend/src/pages/ListingDiscoveryPage.tsx:173`
  - Incident/appeal action panels and status badges: `frontend/src/pages/IncidentDetailPage.tsx:108`, `frontend/src/pages/AppealsPage.tsx:196`
  - Inventory low-stock red badges/alerts: `frontend/src/pages/InventoryPage.tsx:104`, `frontend/src/pages/InventoryPage.tsx:150`
- Manual verification note: exact visual rendering quality and responsive behavior remain **Manual Verification Required**.

## 5. Issues / Suggestions (Severity-Rated)

### Blocker / High

1) **High** - Incident resolution UI cannot satisfy backend closure-code contract
- Conclusion: Fail
- Evidence: `backend/src/main/java/com/demo/app/application/service/IncidentService.java:97`, `frontend/src/api/incidents.ts:37`, `frontend/src/pages/IncidentDetailPage.tsx:119`
- Impact: Moderator/admin resolving from UI can fail for RESOLVED transition, breaking a core incident lifecycle path.
- Minimum actionable fix: extend frontend API and UI action to submit `closureCode` when status is `RESOLVED`; align page form and API typings.
- Minimal verification path: statically ensure `updateIncidentStatus` signature includes closure code and `IncidentDetailPage` collects/sends it; then manual API/UI flow verification.

2) **High** - Risk analytics does not model seller-linked incident semantics required by prompt
- Conclusion: Partial Fail
- Evidence: `backend/src/main/java/com/demo/app/application/service/RiskAnalyticsService.java:68`, `backend/src/main/java/com/demo/app/application/service/RiskAnalyticsService.java:80`, `backend/src/main/java/com/demo/app/persistence/entity/IncidentEntity.java:24`
- Impact: risk dashboards may not represent the stated business signal “repeat incidents per seller in last 30 days,” reducing requirement-fit of operational risk scoring.
- Minimum actionable fix: add explicit seller linkage in incident/order risk data model and compute seller-window metrics directly; expose these factors in risk score payload.
- Minimal verification path: add tests that assert seller-specific incident-window calculations and factor output.

3) **High** - Sensitive at-rest encryption appears incomplete for user PII
- Conclusion: Partial Fail
- Evidence: encrypted phone field exists `backend/src/main/java/com/demo/app/persistence/entity/MemberProfileEntity.java:33`, while user PII fields are plain columns `backend/src/main/java/com/demo/app/persistence/entity/UserEntity.java:28`, `backend/src/main/java/com/demo/app/persistence/entity/UserEntity.java:34`
- Impact: explicit prompt privacy constraint (“encrypt sensitive fields at rest”) is only partially satisfied, exposing privacy/compliance risk.
- Minimum actionable fix: define sensitive-field policy and encrypt/mask relevant user/member fields (at least email/display_name if in-scope) consistently with key management and migration strategy.
- Minimal verification path: static inspection of entity mappings/converters + migration changes for encrypted storage columns.

### Medium

4) **Medium** - Static verifiability weakened by documentation and gate inconsistencies
- Conclusion: Partial Fail
- Evidence: migration count claim `README.md:128` vs `backend/src/main/resources/db/migration/V19__evidence_key_rotation.sql:1`; coverage claims `README.md:65`, `run_tests.sh:22`, `run_tests.sh:41` vs actual gates `backend/pom.xml:171`, `frontend/vitest.config.ts:49`
- Impact: reviewers can be misled about quality gates and deliverable state.
- Minimum actionable fix: synchronize README/test-script comments with actual configured thresholds and migration inventory.

5) **Medium** - Appeal creation does not validate related entity type/id existence on submission
- Conclusion: Partial Fail
- Evidence: `backend/src/main/java/com/demo/app/application/service/AppealService.java:24`, DTO only validates shape `backend/src/main/java/com/demo/app/api/dto/CreateAppealRequest.java:8`
- Impact: orphaned or semantically invalid appeals can be recorded, weakening auditability and workflow integrity.
- Minimum actionable fix: validate `relatedEntityType` against allowed enum and verify referenced entity exists before persisting.

6) **Medium** - Evidence MIME magic validation likely rejects valid WebP files
- Conclusion: Partial Fail
- Evidence: `backend/src/main/java/com/demo/app/api/controller/AppealController.java:121`, `backend/src/main/java/com/demo/app/api/controller/AppealController.java:123`
- Impact: user-facing evidence upload failures for allowed type, reducing appeal workflow reliability.
- Minimum actionable fix: correct WebP signature check to proper RIFF/WEBP offsets and add dedicated tests for positive/negative MIME magic cases.

### Low

7) **Low** - Inventory frontend terminology/workflow drifts from backend document-specific endpoints
- Conclusion: Partial Pass
- Evidence: frontend uses generic adjust flow `frontend/src/api/warehouses.ts:26`, `frontend/src/pages/InventoryPage.tsx:6`; backend exposes dedicated inbound/outbound/stocktake endpoints `backend/src/main/java/com/demo/app/api/controller/InventoryController.java:83`, `backend/src/main/java/com/demo/app/api/controller/InventoryController.java:94`, `backend/src/main/java/com/demo/app/api/controller/InventoryController.java:105`
- Impact: UX may under-communicate required operational document semantics though backend capability exists.
- Minimum actionable fix: expose dedicated UI actions/forms for inbound/outbound/stocktake documents explicitly.

## 6. Security Review Summary

- Authentication entry points: **Pass**
  - Evidence: auth endpoints and JWT filter wiring: `backend/src/main/java/com/demo/app/api/controller/AuthController.java:19`, `backend/src/main/java/com/demo/app/security/JwtAuthFilter.java:22`, `backend/src/main/java/com/demo/app/security/SecurityConfig.java:41`
  - Reasoning: signed-token auth path is clearly implemented and integrated in filter chain.

- Route-level authorization: **Pass**
  - Evidence: route matcher role matrix: `backend/src/main/java/com/demo/app/security/SecurityConfig.java:41`
  - Reasoning: high-risk namespaces (`/risk`, `/audit`, `/users`) are role-gated.

- Object-level authorization: **Partial Pass**
  - Evidence: ownership checks in orders/reservations/incidents/appeals: `backend/src/main/java/com/demo/app/api/controller/OrderController.java:76`, `backend/src/main/java/com/demo/app/api/controller/ReservationController.java:55`, `backend/src/main/java/com/demo/app/api/controller/IncidentController.java:120`, `backend/src/main/java/com/demo/app/api/controller/AppealController.java:52`
  - Reasoning: many object checks exist; however, some business references (e.g., appeal relatedEntity linkage) are not validated for existence.

- Function-level authorization: **Pass**
  - Evidence: method-level `@PreAuthorize` on sensitive operations: `backend/src/main/java/com/demo/app/api/controller/InventoryController.java:68`, `backend/src/main/java/com/demo/app/api/controller/RiskAnalyticsController.java:23`, `backend/src/main/java/com/demo/app/api/controller/AuditController.java:14`

- Tenant/user data isolation: **Partial Pass**
  - Evidence: user-scoped fetch paths `.../my` and ownership guards across major controllers.
  - Reasoning: strong per-user guards in core flows; no multi-tenant boundary model detected (Not Applicable beyond single-tenant role model).

- Admin/internal/debug protection: **Pass**
  - Evidence: admin-only audit/risk/users paths in security config and controller annotations: `backend/src/main/java/com/demo/app/security/SecurityConfig.java:60`, `backend/src/main/java/com/demo/app/api/controller/AuditController.java:14`

## 7. Tests and Logging Review

- Unit tests: **Pass**
  - Evidence: backend and frontend unit suites exist and are structured: `backend/pom.xml:103`, `frontend/vitest.config.ts:34`, `unit_tests/backend/java/com/demo/app/api/controller/AuthControllerTest.java:33`, `unit_tests/frontend/pages/IncidentDetailPage.test.tsx:36`

- API / integration tests: **Pass**
  - Evidence: pytest suite breadth: `API_tests/test_rbac.py:6`, `API_tests/test_object_auth.py:6`, `API_tests/test_incidents.py:70`, `API_tests/test_hardening.py:57`

- Logging categories / observability: **Partial Pass**
  - Evidence: dedicated audit and service logs: `backend/src/main/java/com/demo/app/infrastructure/audit/AuditService.java:15`, `backend/src/main/java/com/demo/app/application/service/IncidentEscalationService.java:22`, `backend/src/main/java/com/demo/app/application/service/InventoryService.java:69`
  - Note: structured logging is present but not uniformly rich across all domains.

- Sensitive-data leakage risk in logs/responses: **Partial Pass**
  - Evidence: audit redaction implemented: `backend/src/main/java/com/demo/app/infrastructure/audit/AuditAspect.java:47`
  - Remaining risk: PII-at-rest handling is inconsistent (see High issue #3), and runtime response/log content still requires manual verification under real error paths.

## 8. Test Coverage Assessment (Static Audit)

### 8.1 Test Overview
- Unit tests exist: backend JUnit + frontend Vitest.
- API/integration tests exist: pytest + requests.
- Test frameworks/entry points:
  - Backend: `backend/pom.xml:103`
  - Frontend: `frontend/vitest.config.ts:34`
  - API tests: `API_tests/conftest.py:1`
- Documentation provides test command: `README.md:58`
- Coverage commands/gates documented but inconsistent with configs (see Issue #4).

### 8.2 Coverage Mapping Table

| Requirement / Risk Point | Mapped Test Case(s) | Key Assertion / Fixture / Mock | Coverage Assessment | Gap | Minimum Test Addition |
|---|---|---|---|---|---|
| Auth login + token path | `API_tests/test_auth.py:9`, `unit_tests/backend/java/com/demo/app/api/controller/AuthControllerTest.java:49` | Login success and token checks | sufficient | None material | Keep regression tests for token expiry edge cases |
| Login lockout (10/hr + lockout) | `API_tests/test_rate_limiting.py:16`, `unit_tests/backend/java/com/demo/app/infrastructure/ratelimit/LoginAttemptServiceTest.java:20` | Repeated failed logins trigger lockout semantics | basically covered | Status-code tolerance is broad (may hide regressions) | Tighten expected status/message contract |
| Request rate limiting (60/min) | `unit_tests/backend/java/com/demo/app/infrastructure/ratelimit/RateLimitServiceTest.java:9`, `API_tests/test_rate_limiting.py:7` | Token-bucket behavior tested | basically covered | API-level tests do not always drive to hard 429 boundary | Add deterministic boundary test for 61st request |
| Route authorization (401/403) | `API_tests/test_rbac.py:12`, `API_tests/test_rbac.py:19` | protected endpoint no auth, member denied admin endpoints | sufficient | None material | Add matrix snapshot by endpoint group |
| Object-level authorization | `API_tests/test_object_auth.py:6`, `API_tests/test_object_auth.py:63`, `API_tests/test_object_auth.py:102` | cross-user denial for orders/reservations/incidents/appeals | sufficient | Limited seller-specific fulfillment object tests | Add fulfillment ownership tests for seller/non-owner |
| Discovery filters + sorting | `API_tests/test_discovery_filters.py:6`, `API_tests/test_discovery_filters.py:43`, `unit_tests/frontend/pages/DiscoveryPage.test.tsx:62` | keyword/neighborhood/price/sqft/layout/radius checks | basically covered | No strict assertion for distance ordering correctness in API integration | Add deterministic ordering assertion fixture |
| Incident SLA + closure code | `API_tests/test_incidents.py:70`, `API_tests/test_incidents.py:82` | resolve requires closureCode; resolve succeeds with closureCode | sufficient (backend) | Frontend tests encode wrong contract (status-only) | Add frontend API/page tests requiring closureCode for RESOLVED |
| Appeals evidence constraints | `API_tests/test_appeals.py:55`, `API_tests/test_object_auth.py:128` | basic appeal/evidence exposure checks | insufficient | No robust tests for file type/size/max-count/WEBP magic behavior | Add multipart boundary + MIME magic suites |
| Inventory inbound/outbound/stocktake | `API_tests/test_hardening.py:57`, `API_tests/test_hardening.py:69`, `API_tests/test_hardening.py:81` | dedicated endpoint behavior validated | basically covered | Frontend coverage absent for dedicated workflows | Add UI/API tests for explicit document flows |
| TLS/proxy security headers | `API_tests/test_tls_enforcement.py:1` | HTTPS and security headers tested | basically covered | Host exposure assumptions still environment-dependent | Manual network-level verification checklist |
| At-rest encryption of sensitive fields | No direct tests found for user PII encryption at entity/storage level | N/A | missing | Severe privacy regressions could pass test suite | Add persistence-level tests for encrypted-at-rest policy enforcement |

### 8.3 Security Coverage Audit
- Authentication: **Basically covered**
  - API + unit tests cover login/jwt/lockout, but edge contracts could be tighter.
- Route authorization: **Covered**
  - Multiple RBAC tests exercise deny/allow paths for key roles.
- Object-level authorization: **Covered for major flows**
  - Good matrix in `test_object_auth.py`; residual gaps for certain fulfillment permutations.
- Tenant/data isolation: **Basically covered**
  - Single-tenant role model; user isolation checks present for key objects.
- Admin/internal protection: **Covered**
  - `/risk`, `/audit`, `/users` path expectations exercised.

### 8.4 Final Coverage Judgment
- **Partial Pass**
- Boundary explanation:
  - Major auth/RBAC/object-auth and core operational flows are covered.
  - Important uncovered area remains: cross-layer incident resolve contract (frontend), full appeal evidence validation matrix, and explicit at-rest encryption policy tests. Severe defects in these areas could persist while many tests still pass.

## 9. Final Notes
- This is a static-only audit. No runtime success claims are made.
- Most foundational architecture is present and substantial.
- Acceptance risk is concentrated in requirement-semantic gaps (risk analytics, privacy encryption scope) and cross-layer contract drift (incident resolve), plus documentation verifiability inconsistencies.

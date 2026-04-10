# Delivery Acceptance and Project Architecture Audit (Static-Only)

Date: 2026-04-10
Scope root: /home/mint/Desktop/projects/task-12/repo

## 1. Verdict
- Overall conclusion: **Partial Pass**
- Rationale: The repository is substantial and maps to most prompt domains (tiers, discovery, incidents, appeals, inventory, fulfillment, deletion, audit, rate limits), but there are material authorization and requirement-fit gaps, including cross-object exposure risks and incomplete implementation of anomaly-flag ingestion / compensation semantics.

## 2. Scope and Static Verification Boundary
- What was reviewed:
  - Documentation and manifests: README.md, docker-compose.yml, run_tests.sh, backend/pom.xml, frontend/package.json, frontend/vitest.config.ts, API_tests/requirements.txt.
  - Backend security/authz/authn: SecurityConfig, JWT, rate limit, login lockout, controllers/services/repositories/migrations.
  - Frontend feature pages: discovery, listing detail, checkout, inventory, routing/guards.
  - Test assets: API_tests/*.py, unit_tests/backend/*.java, unit_tests/frontend/*.ts(x).
- What was not reviewed:
  - Runtime behavior under real execution, timing, network behavior, browser rendering behavior, Docker orchestration outcomes.
  - Dynamic race conditions, true concurrent write behavior, production deployment hardening beyond static config.
- What was intentionally not executed:
  - Project startup, Docker, tests, external services (per instruction).
- Claims requiring manual verification:
  - Effective TLS-only enforcement in deployed topology and direct backend network path controls.
  - Real SLA scheduler timing behavior in live clock conditions.
  - True operational behavior of local file-upload storage lifecycle and disk permission model.

## 3. Repository / Requirement Mapping Summary
- Prompt core goal mapped: offline cultural marketplace operations for multi-role users with listing discovery, tier benefits, inventory/fulfillment control, incidents/escalation, appeals/evidence, RBAC, signed sessions, rate limits, audit/privacy controls.
- Main implementation areas mapped:
  - Backend: role-gated REST controllers and service layer, JWT auth, Bucket4j rate limiting, login attempt tracking, Flyway migrations for tiers/benefits/listings/inventory/incidents/appeals/fulfillment/risk/audit/deletion.
  - Frontend: role-aware routes/pages for discovery, listing details, checkout benefit messaging, incidents, appeals, inventory/reservations.
  - Tests: API tests for RBAC, object auth, filters, incidents, appeals, idempotency, hardening; backend unit tests for services/security/encryption/audit.

## 4. Section-by-section Review

### 4.1 Hard Gates

#### 1.1 Documentation and static verifiability
- Conclusion: **Pass**
- Rationale: Startup, services, test command, architecture, seeded users, and core feature claims are documented with corresponding code structure.
- Evidence:
  - README.md:9, README.md:12, README.md:49, README.md:52
  - docker-compose.yml:1, docker-compose.yml:26
  - run_tests.sh:30, run_tests.sh:49, run_tests.sh:60, run_tests.sh:78
  - backend/pom.xml:111
- Manual verification note: Runtime claims in README about fully passing flows remain manual until executed.

#### 1.2 Material deviation from prompt
- Conclusion: **Partial Pass**
- Rationale: Most domains are implemented, but anomaly-flag loop is incomplete at API boundary (scoring reads anomaly event types but no ingestion endpoints), and some requirement semantics are weaker than prompt intent.
- Evidence:
  - backend/src/main/java/com/demo/app/application/service/RiskAnalyticsService.java:39, :117, :123, :129
  - backend/src/main/java/com/demo/app/api/controller/RiskAnalyticsController.java:15, :22, :36, :42
  - grep over backend controllers: no controller endpoint calling recordEvent(...)
- Manual verification note: If anomaly event ingestion is performed only via out-of-band DB writes, this is architectural debt and must be explicitly documented.

### 4.2 Delivery Completeness

#### 2.1 Core explicit requirements coverage
- Conclusion: **Partial Pass**
- Rationale:
  - Implemented: discovery filters/sorts + recent/trending, member tiers/benefits, stock reservation/hold/rollback, low-stock threshold behavior, incidents+SLA escalation, appeals upload constraints, rate limiting, lockout, account deletion cooling-off, audit retention.
  - Gaps: anomaly exception loop not fully wired; object-level access boundaries are incomplete for certain modules.
- Evidence:
  - Discovery UI + recent/trending: frontend/src/pages/ListingDiscoveryPage.tsx:7, :8, :42, :139, :411
  - Advanced search: backend/src/main/java/com/demo/app/application/service/ListingService.java:121
  - Tier bands: backend/src/main/resources/db/migration/V10__correct_tiers_listing_incident_risk.sql:2
  - Benefit scope/exclusion/date/seller/category: backend/src/main/resources/db/migration/V12__benefit_scope_incident_closure.sql:2
  - Reservation hold/confirm/cancel/expire: backend/src/main/java/com/demo/app/application/service/ReservationService.java:41, :84, :126, :164
  - Low stock strict >=5: backend/src/main/resources/db/migration/V14__strict_low_stock_threshold.sql:2
  - Incident SLA escalation: backend/src/main/java/com/demo/app/infrastructure/scheduler/ScheduledTasks.java:34; backend/src/main/java/com/demo/app/persistence/repository/IncidentRepository.java:19, :23
  - Appeal evidence constraints: backend/src/main/java/com/demo/app/api/controller/AppealController.java:96, :134; backend/src/main/resources/db/migration/V15__appeal_evidence_limit.sql:1

#### 2.2 End-to-end 0->1 deliverable vs partial/demo
- Conclusion: **Pass**
- Rationale: Complete multi-module full-stack monorepo with backend/frontend/proxy/db migrations/tests and role workflows; not a single-file demo.
- Evidence:
  - README.md:3, :9, :49
  - backend/src/main/java/com/demo/app/api/controller/*.java
  - frontend/src/pages/*.tsx
  - API_tests/*.py, unit_tests/backend/java/*.java, unit_tests/frontend/**/*.test.tsx

### 4.3 Engineering and Architecture Quality

#### 3.1 Structure and module decomposition
- Conclusion: **Pass**
- Rationale: Layered backend and organized frontend, clear module responsibilities, migration-driven schema evolution.
- Evidence:
  - README.md architecture section
  - backend package split under api/application/domain/persistence/security/infrastructure
  - frontend/src/{api,state,components,features,pages}

#### 3.2 Maintainability/extensibility
- Conclusion: **Partial Pass**
- Rationale: Generally extensible, but some security-sensitive ownership checks are inconsistent and not centralized; risk event ingestion missing from API undermines maintainable risk loop.
- Evidence:
  - Ownership checks present in reservations/incidents/appeals: ReservationController.java:58, IncidentController.java:118, AppealController.java:52
  - Missing equivalent object checks in fulfillment read endpoints: FulfillmentController.java:59, :64
  - Risk event write path not exposed: RiskAnalyticsService.java:39 (method exists), no controller caller

### 4.4 Engineering Details and Professionalism

#### 4.1 Error handling/logging/validation/API design
- Conclusion: **Partial Pass**
- Rationale:
  - Positive: centralized exception mapping, redacted audit payload strategy, structured rate-limit responses.
  - Negative: several DTOs/services rely on RuntimeException/weak validation where typed validation is expected.
- Evidence:
  - Global exception handling: backend/src/main/java/com/demo/app/api/controller/GlobalExceptionHandler.java:46, :53, :56, :64
  - Audit redaction: backend/src/main/java/com/demo/app/infrastructure/audit/AuditAspect.java:53-56
  - Weak validation in appeals create fields: backend/src/main/java/com/demo/app/api/dto/CreateAppealRequest.java:6-8

#### 4.2 Product-grade vs demo-grade
- Conclusion: **Partial Pass**
- Rationale: Product-like breadth and persistence model exist, but key security boundaries and some prompt-critical semantics are not production-hard.
- Evidence:
  - Broad domain implementation across migrations/services/controllers/tests
  - Security/authorization gap examples in Issues section

### 4.5 Prompt Understanding and Requirement Fit

#### 5.1 Business goal/constraints fit
- Conclusion: **Partial Pass**
- Rationale: Strong alignment with core flows, offline/no third-party pattern, and role model; however, missing anomaly ingestion path and weak object-level guardrails in some endpoints reduce requirement fidelity for internal risk/compliance operations.
- Evidence:
  - Offline/tls topology intent: README.md:3-4, proxy/nginx.conf:17, :22
  - No third-party dependencies in app packages (static review)
  - Risk/anomaly loop gap: RiskAnalyticsService.java:117-132 with no event-ingestion API

### 4.6 Aesthetics (frontend-only/full-stack)

#### 6.1 Visual and interaction quality
- Conclusion: **Pass (Static UI code review only)**
- Rationale: Discovery, filters, cards, alerts, tabs, and action feedback states exist; functional areas are segmented and interaction controls are present.
- Evidence:
  - Discovery/filter/sort/recent/trending UI: frontend/src/pages/ListingDiscoveryPage.tsx:139, :394, :411
  - Checkout benefit explanation and non-stackable messaging: frontend/src/pages/CheckoutPage.tsx:49-84
  - Inventory low-stock and tabs: frontend/src/pages/InventoryPage.tsx:92-117
- Manual verification note: Pixel-level rendering quality and responsive behavior require browser execution.

## 5. Issues / Suggestions (Severity-Rated)

### Blocker / High first

1) Severity: **High**
- Title: Fulfillment object-level authorization missing for read endpoints
- Conclusion: **Fail**
- Evidence:
  - backend/src/main/java/com/demo/app/security/SecurityConfig.java:58 (sellers allowed GET /api/fulfillments/**)
  - backend/src/main/java/com/demo/app/api/controller/FulfillmentController.java:59, :64 (no ownership/warehouse scoping checks)
  - backend/src/main/java/com/demo/app/application/service/FulfillmentService.java:114-121 (direct fetch by orderId/fulfillmentId)
- Impact: A seller could read fulfillment details/steps for unrelated orders, violating object authorization and data isolation.
- Minimum actionable fix: Enforce object/tenant scope in FulfillmentController service calls (e.g., seller can only access fulfillments linked to their products/orders) and add explicit 403 tests.

2) Severity: **High**
- Title: Inventory data access lacks seller object scoping
- Conclusion: **Fail**
- Evidence:
  - backend/src/main/java/com/demo/app/security/SecurityConfig.java:57 (SELLER allowed /api/inventory/**)
  - backend/src/main/java/com/demo/app/api/controller/InventoryController.java:30, :38 (returns product/low-stock data without seller ownership filters)
  - backend/src/main/java/com/demo/app/application/service/InventoryService.java:31-38 (repository passthrough)
- Impact: Seller users can query inventory and low-stock signals beyond their own catalog/warehouses.
- Minimum actionable fix: Add seller ownership constraints to inventory queries (join product.seller_id) and return 403/filtered results accordingly; add API tests for cross-seller denial.

3) Severity: **High**
- Title: Exception-and-anomaly loop incomplete at API boundary
- Conclusion: **Fail**
- Evidence:
  - backend/src/main/java/com/demo/app/application/service/RiskAnalyticsService.java:39 (recordEvent exists)
  - backend/src/main/java/com/demo/app/application/service/RiskAnalyticsService.java:117-132 (scores MISSED_PICKUP_CHECKIN/BUDDY_PUNCHING/MISIDENTIFICATION)
  - backend/src/main/java/com/demo/app/api/controller/RiskAnalyticsController.java:22, :36, :42 (no event ingestion endpoint)
- Impact: Prompt-required anomaly flag workflow is only partially implemented; operational teams cannot reliably feed exception signals through supported APIs.
- Minimum actionable fix: Add protected endpoint(s) to record risk events/anomaly flags with validation and provenance, plus tests for anomaly-to-score propagation.

4) Severity: **High**
- Title: Appeal evidence stored unencrypted on local filesystem
- Conclusion: **Partial Fail**
- Evidence:
  - backend/src/main/java/com/demo/app/api/controller/AppealController.java:146-150 (writes raw file to uploads/appeals/...)
  - backend/src/main/java/com/demo/app/persistence/entity/AppealEvidenceEntity.java:24 (storedPath persisted)
- Impact: Sensitive evidence may rest unencrypted at filesystem level, weakening privacy controls for personal/safety evidence artifacts.
- Minimum actionable fix: Encrypt evidence at rest (file-level encryption or encrypted volume with key management) and document retention/deletion handling.

### Medium

5) Severity: **Medium**
- Title: Benefit issuance/redemption records are not strongly tied to order/ticket entities
- Conclusion: **Partial Fail**
- Evidence:
  - backend/src/main/resources/db/migration/V3__member_tiers_benefits.sql:48, :59, :69 (reference is VARCHAR only)
  - backend/src/main/java/com/demo/app/application/service/BenefitService.java:46-102 (uses free-form reference string)
- Impact: Weaker traceability/integrity versus prompt expectation of records tied to orders/tickets.
- Minimum actionable fix: Add typed foreign keys or polymorphic reference model with referential checks; validate linkage at redemption/issuance time.

6) Severity: **Medium**
- Title: Appeal create request lacks strict validation on related entity fields
- Conclusion: **Fail**
- Evidence:
  - backend/src/main/java/com/demo/app/api/dto/CreateAppealRequest.java:6-8
- Impact: Null/blank relatedEntityType or missing relatedEntityId can slip to persistence failures or inconsistent records.
- Minimum actionable fix: Add @NotBlank to relatedEntityType and @NotNull/@Positive to relatedEntityId; add 400 tests.

7) Severity: **Medium**
- Title: Test-coverage gates can overstate confidence for high-risk backend areas
- Conclusion: **Partial Fail**
- Evidence:
  - backend/pom.xml:144-146 (exclude controllers/config/scheduler from JaCoCo)
  - backend/pom.xml:179 (minimum line ratio 0.50)
  - frontend/vitest.config.ts:25, :33 (narrow include/exclude scope)
- Impact: Critical edge behavior may remain unmeasured while coverage metrics appear acceptable.
- Minimum actionable fix: Raise backend gate and include security-critical controllers/schedulers in measured scope; expand frontend coverage include to route-level security paths.

8) Severity: **Medium**
- Title: TLS requirement partially architecture-based, not end-to-end enforced in API test path
- Conclusion: **Cannot Confirm Statistically**
- Evidence:
  - proxy/nginx.conf:17, :22 (TLS listener)
  - API_tests/conftest.py:6 (default base URL is http://backend:8080/api)
  - API_tests/verify.py:7, :8 (both backend HTTP and proxy HTTPS paths used)
- Impact: Requirement says TLS for local network transmission; static artifacts show mixed direct HTTP path usage.
- Minimum actionable fix: Enforce backend access only via TLS proxy for non-internal channels and document trusted internal network boundaries explicitly.

### Low

9) Severity: **Low**
- Title: README includes runtime-success language beyond static proof
- Conclusion: **Cannot Confirm Statistically**
- Evidence:
  - README.md:15, :350
- Impact: Reviewer may over-assume runtime correctness from docs.
- Minimum actionable fix: Mark runtime sections as “expected” and tie to reproducible command outputs/artifacts.

## 6. Security Review Summary

- authentication entry points: **Pass**
  - Evidence: AuthController login/register endpoints (backend/src/main/java/com/demo/app/api/controller/AuthController.java:22, :29), JWT filter chain (backend/src/main/java/com/demo/app/security/SecurityConfig.java:68).
- route-level authorization: **Partial Pass**
  - Evidence: centralized route matchers (backend/src/main/java/com/demo/app/security/SecurityConfig.java:44-64), admin-only risk/audit routes (RiskAnalyticsController.java:16, AuditController.java:14).
  - Gap: seller-allowed GET fulfillments and broad seller inventory access without ownership scoping.
- object-level authorization: **Partial Pass**
  - Evidence (good): reservations/incidents/appeals ownership checks (ReservationController.java:58, IncidentController.java:118, AppealController.java:52).
  - Evidence (gap): fulfillment/inventory read paths without object scoping (FulfillmentController.java:59, :64; InventoryController.java:30, :38).
- function-level authorization: **Partial Pass**
  - Evidence: multiple @PreAuthorize usages in controllers.
  - Gap: some methods rely only on URL matcher-level role checks with no business-object constraints.
- tenant / user isolation: **Partial Pass**
  - Evidence: order ownership enforcement (OrderController.java:80), incident/appeal ownership checks.
  - Gap: cross-seller visibility possible via inventory and fulfillment GET.
- admin / internal / debug protection: **Pass**
  - Evidence: risk and audit controllers class-level admin preauth (RiskAnalyticsController.java:16, AuditController.java:14).

## 7. Tests and Logging Review

- Unit tests: **Pass (with noted scope caveats)**
  - Evidence: unit tests for JWT/rate limit/encryption/audit/services under unit_tests/backend/java.
- API / integration tests: **Partial Pass**
  - Evidence: API_tests include RBAC/object auth/discovery/incidents/appeals/rate/idempotency/hardening.
  - Gaps: no explicit negative tests for fulfillment object-level access (cross-order by seller), no anomaly event ingestion tests.
- Logging categories / observability: **Partial Pass**
  - Evidence: SLF4J logging in escalation/inventory/account deletion services; structured audit storage.
  - Gap: observability around failed authorization decisions and evidence file handling lifecycle is limited.
- Sensitive-data leakage risk in logs/responses: **Partial Pass**
  - Evidence (good): AuditAspect redacts payload content (AuditAspect.java:53-56), evidence listing strips storedPath (API_tests/test_object_auth.py:117).
  - Risk: raw evidence files stored locally unencrypted; no static proof of secure file-at-rest handling.

## 8. Test Coverage Assessment (Static Audit)

### 8.1 Test Overview
- Unit tests exist: Yes.
  - Backend JUnit source path configured at backend/pom.xml:111.
- API/integration tests exist: Yes (pytest).
  - Framework deps: API_tests/requirements.txt:1-3.
  - Entry points documented: README.md:49 and run_tests.sh:30, :49, :78.
- Frontend unit tests exist: Yes.
  - Vitest include pattern and thresholds: frontend/vitest.config.ts:25, :47-48.
- Documentation provides test commands: Yes.
  - README.md:49-52; run_tests.sh orchestrates suites.

### 8.2 Coverage Mapping Table
| Requirement / Risk Point | Mapped Test Case(s) | Key Assertion / Fixture / Mock | Coverage Assessment | Gap | Minimum Test Addition |
|---|---|---|---|---|---|
| Auth login/token/me | API_tests/test_auth.py:8, :56; unit_tests/backend/java/com/demo/app/security/JwtServiceTest.java:26 | JWT has 3 parts and role claim | basically covered | Token revocation/session invalidation not covered | Add token revocation/disabled-user tests |
| Route RBAC | API_tests/test_rbac.py:7, :16, :22, :40; RbacIntegrationTest.java:57+ | 401/403 role checks on protected routes | sufficient | None major statically | Add regression matrix for new endpoints |
| Object-level auth incidents/appeals/reservations | API_tests/test_object_auth.py:60, :82, :100 | 403 for cross-user access | sufficient | Fulfillment/inventory object scoping missing tests | Add seller cross-order fulfillment GET forbidden tests; seller cross-catalog inventory forbidden tests |
| Rate limiting and login lockout | API_tests/test_hardening.py:8, :27; API_tests/test_rate_limiting.py:16 | 65 requests include 429; lockout messaging/status | basically covered | No fairness/eviction race tests | Add per-key isolation + bucket eviction timing tests |
| Discovery filters + distance sort | API_tests/test_discovery_filters.py:20, :34, :40, :50 | Range/layout/radius and distance response assertions | basically covered | Sort correctness not deeply asserted | Add deterministic distance ordering assertions with controlled fixtures |
| Tier spend bands + member profile | API_tests/test_member_tiers.py:16, :29 | Strict 0-499/500-1499/1500+ assertions | sufficient | None major | Add boundary-value tests around 499/500/1499/1500 in service layer |
| Incident SLA + closure-code semantics | API_tests/test_incidents.py:70, :82; IncidentEscalationServiceTest.java:58, :91 | Resolve requires closureCode; escalation level transitions | sufficient | Runtime scheduler timing not executed | Add scheduler-triggered integration tests with frozen clock |
| Appeals review and evidence constraints | API_tests/test_appeals.py:25, :37; AppealController.java:96, :134 | Review states and file-size/count checks | insufficient | API tests do not upload real files to assert MIME/size/count behavior end-to-end | Add multipart upload tests (valid image/pdf, bad magic, >10MB, >5 files) |
| Inventory hold/confirm/cancel rollback | API_tests/test_inventory.py:31, :49, :72; ReservationServiceTest.java:118, :138 | Confirm/cancel behavior and idempotency | sufficient | Concurrency race path untested | Add concurrent reserve/confirm collision tests |
| Audit retention and trail | API_tests/test_audit_trail.py:11, :33; AuditServiceTest.java:48, :91 | Mutations create audit entries; purge expired | basically covered | No sensitive payload leak regression test | Add test asserting audit newValue excludes request payload secrets |
| Fulfillment authorization boundary | API_tests/test_fulfillment.py:8, :45 | Happy-path fulfillment lifecycle | **missing (security negative path)** | Severe defect could remain undetected | Add tests: seller GET /fulfillments/order/{id} for foreign order -> 403 |
| Risk anomaly loop completeness | none for anomaly event ingestion | N/A | **missing** | No testable ingestion path exists | Add API endpoint + tests for recording anomaly events and score impact |

### 8.3 Security Coverage Audit
- authentication: **Basically covered** (test_auth + JwtServiceTest).
- route authorization: **Covered** (test_rbac + RbacIntegrationTest).
- object-level authorization: **Insufficient** (covered for incidents/appeals/reservations, not for fulfillment/inventory cross-object scenarios).
- tenant / data isolation: **Insufficient** (same reason; severe leaks could survive current tests).
- admin / internal protection: **Basically covered** (risk/audit route tests), but no broad fuzzing of admin-only endpoints.

### 8.4 Final Coverage Judgment
- **Partial Pass**
- Covered major risks:
  - Core RBAC/authn/rate limit/lockout, many business paths (tiers, discovery filters, incidents, reservations, audit basics).
- Uncovered major risks allowing severe defects to pass:
  - Fulfillment and inventory object-level isolation gaps.
  - Missing anomaly-event ingestion and related testability.
  - Limited end-to-end evidence upload validation coverage.

## 9. Final Notes
- This audit is static-only and evidence-traceable; no runtime claims are made as proven without execution.
- The project is close to acceptance for breadth, but the highlighted High-severity security/requirement-fit issues should be treated as release blockers for compliance-grade deployment.
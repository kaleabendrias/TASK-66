# Required Document Description: Business Logic Questions Log

## 1. Incident resolution closure requirement
- Question: When changing an incident to `RESOLVED`, should a closure code be mandatory and validated server-side?
- My Understanding: Resolution without closure semantics weakens audit quality and post-incident analytics; closure code should be required.
- Solution: Enforce `closureCode` validation at status transition and align frontend flow to always send it for resolution.

## 2. Seller-scoped risk analytics semantics
- Question: The business prompt emphasizes repeat incidents per seller in a rolling window; should risk scoring be tied to seller exposure rather than only reporter/assignee identities?
- My Understanding: Seller risk must be computed from incidents filed against that seller, not merely by who opened or handled incidents.
- Solution: Add explicit `seller_id` linkage in incident data model and use seller-window aggregation in risk scoring factors.

## 3. Sensitive-field encryption scope
- Question: Which user/member fields are considered sensitive and must be encrypted at rest?
- My Understanding: At minimum, personally identifying fields (email, display name, phone data) should follow encryption policy with deterministic lookup support where needed.
- Solution: Apply converter-based encryption to designated PII fields and add lookup hash columns for uniqueness/query requirements.

## 4. Benefit reference integrity
- Question: Should benefit issuance/redemption require strict typed references (ORDER/INCIDENT) and referential validity at write time?
- My Understanding: Ledger integrity depends on explicit and valid origin references; permissive references create reconciliation ambiguity.
- Solution: Enforce reference type constraints in migrations and service-layer existence checks before persistence.

## 5. Appeal relationship validation
- Question: For appeal creation, must `relatedEntityType` and `relatedEntityId` be validated against real existing records?
- My Understanding: Accepting unchecked related entities creates orphaned appeals and weakens workflow traceability.
- Solution: Normalize allowed entity types and validate existence through type-mapped repository checks.

## 6. Evidence upload validation detail
- Question: Is MIME validation required beyond extension/content-type headers for appeal evidence files?
- My Understanding: Header-only checks are insufficient; magic-byte validation is needed to prevent spoofed file uploads.
- Solution: Implement strict signature checks (JPEG/PNG/GIF/PDF/WebP) with correct byte offsets and bounded file limits.

## 7. Inventory operation semantics
- Question: Should inventory UI expose dedicated document-driven operations (`INBOUND`, `OUTBOUND`, `STOCKTAKE`) instead of one generic adjustment action?
- My Understanding: Distinct operations better reflect warehouse controls, auditing, and operator intent.
- Solution: Provide explicit forms/actions per movement type and route to dedicated backend endpoints.

## 8. Documentation vs configured quality gates
- Question: Must README and test scripts exactly match configured migration counts and coverage gates?
- My Understanding: Misalignment reduces review confidence and can cause incorrect acceptance assumptions.
- Solution: Keep documentation synchronized with actual migration inventory and CI/test threshold settings.

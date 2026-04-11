# Heritage Marketplace Operations Management System - Design Document

## 1. Overview
This system is an offline-first operations platform for a local heritage marketplace that handles listings, member tier benefits, incidents, appeals, inventory, and fulfillment workflows.

Primary goals:
- Provide role-aware workflows for Guest, Member, Seller, Warehouse Staff, Moderator, and Administrator.
- Support reliable transactional operations (reservation holds, fulfillment steps, state transitions).
- Enforce auditability, security boundaries, and compliance constraints in a local deployment model.

## 2. Business Scope
Core business domains:
- Listing Discovery: keyword search, neighborhood/radius filtering, date windows, sorting, trending, and recent searches.
- Membership and Benefits: tier evaluation, benefit package scoping, stacking/mutual exclusion, issuance/redemption records.
- Inventory and Fulfillment: multi-warehouse stock, movement documents, reservation holds, low-stock alerts, fulfillment state flow.
- Incidents and Appeals: emergency ticketing, SLA timers/escalation, evidence uploads, review and closure loops.
- Risk and Audit: local risk scoring and immutable audit trails.

## 3. Architectural Style
High-level architecture:
- Frontend: React + TypeScript single-page app.
- Backend: Spring Boot REST APIs with layered structure (controller -> service -> repository).
- Database: PostgreSQL with Flyway migrations.
- Reverse Proxy: TLS termination and routing through Nginx.

Design principles:
- Clear separation of concerns by domain and role.
- State-machine driven workflows for sensitive lifecycle transitions.
- Idempotency and conflict-safe write operations for operational APIs.
- Security-first enforcement at route, function, and object levels.

## 4. Roles and Access Model
- Guest: view public listings and discovery content.
- Member: place orders, create incidents/appeals, redeem benefits, manage own profile.
- Seller: manage own catalog/listings; limited inventory/fulfillment visibility by ownership.
- Warehouse Staff: inventory operations, reservation processing, fulfillment execution.
- Moderator: incident triage, escalation handling, appeal review.
- Administrator: full internal access (audit, risk analytics, users, policy operations).

Access control layers:
- Authentication: JWT bearer tokens.
- Route-level authorization: role-based endpoint guarding.
- Object-level authorization: ownership and scope checks in service/controller logic.

## 5. Domain Workflows
### 5.1 Discovery
- Search inputs are translated into backend query parameters.
- Results support sorting by newest, price, distance, and popularity.
- Frontend stores recent searches locally (bounded list).

### 5.2 Benefits
- Member tier is derived from spend thresholds.
- Benefit eligibility includes scope constraints (category/seller/date).
- Mutual exclusion groups prevent invalid stacking.

### 5.3 Inventory and Fulfillment
- Inventory writes produce movement records with operator and reference document.
- Reservation hold lifecycle: HELD -> CONFIRMED/CANCELLED/EXPIRED.
- Fulfillment progression follows allowed step transitions.

### 5.4 Incident and Appeal
- Incident creation sets SLA deadlines and optional location notes.
- Escalation jobs process unacknowledged/unresolved incidents.
- Appeals accept evidence under file type/size/count constraints.

## 6. Data and Compliance
- Sensitive data handling:
  - Masked display where required (e.g., phone).
  - Encrypted-at-rest strategy for designated sensitive fields.
- Audit retention target: 2 years.
- Account deletion supports cooling-off workflow before final processing.

## 7. Non-Functional Requirements
- Offline/local operation with no dependency on third-party online integrations.
- Operational transparency through structured logs and audit events.
- Maintainability via migration-based schema evolution and modular code boundaries.

## 8. Risks and Controls
Primary risks:
- Contract drift between frontend and backend for strict transitions.
- Role/ownership regressions in endpoints with shared route families.
- Data privacy regressions in persistence and logs.

Controls:
- API and unit tests for security and lifecycle constraints.
- Validation and conflict handling for input and transition boundaries.
- Consistent domain-level invariants in service layer.

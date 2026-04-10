# Demo Monorepo — Spring Boot + React + PostgreSQL

A self-contained, offline-capable monorepo with a layered Spring Boot backend,
React frontend, PostgreSQL database, and TLS reverse proxy. Zero external
dependencies beyond Docker and Compose.

---

## Quick Start

```bash
docker compose up --build
```

That is the only command. Flyway runs 15 migrations on first start, seeds all
demo data, and the frontend builds with the TLS proxy API URL baked in.

---

## Services and Ports

| Service       | Container | Host Port    | URL                            |
|---------------|-----------|--------------|--------------------------------|
| PostgreSQL 16 | 5432      | internal     | not exposed to host            |
| Spring Boot   | 8080      | internal     | not exposed to host            |
| React (Vite)  | 3000      | internal     | not exposed to host            |
| TLS Proxy     | 8443      | **8443**     | `https://localhost:8443`        |

> Only the TLS proxy is exposed. All client traffic routes through
> `https://localhost:8443`. No `.env` files exist anywhere.

---

## Seeded Demo Credentials

All users share password **`password123`**.

| Username    | Role               | Capabilities                                    |
|-------------|--------------------|-------------------------------------------------|
| `guest`     | GUEST              | Browse public listings and products             |
| `member`    | MEMBER             | Orders, reservations, tier benefits, incidents   |
| `seller`    | SELLER             | Manage products, listings, view inventory        |
| `warehouse` | WAREHOUSE_STAFF    | Inventory ops, stock adjustments, fulfillment    |
| `moderator` | MODERATOR          | Incidents, escalations, appeals, content review  |
| `admin`     | ADMINISTRATOR      | Full access: users, audit, risk, compliance      |

---

## Running Tests

```bash
./run_tests.sh
```

Executes three isolated suites inside Docker containers and exits non-zero on
any failure:

| Suite              | Location                | Runner           | Count | Coverage Gate |
|--------------------|-------------------------|------------------|-------|---------------|
| Backend unit       | `unit_tests/backend/`   | JUnit 5 + JaCoCo | 118   | >= 50% lines (expanding) |
| Frontend unit      | `unit_tests/frontend/`  | Vitest + v8      | 110   | >= 90% lines  |
| API integration    | `API_tests/`            | pytest + requests| 78+   | >= 90% lines  |
| **Total**          |                         |                  |**306+**|              |

### What the Tests Cover

- **RBAC** — every role tested against every protected endpoint
- **Token lifecycle** — generation, expiry, tampering, claim extraction
- **Rate limiting** — 60 req/min bucket, eviction, independent keys
- **Login lockout** — 10 failures/hr threshold, 15-minute cooldown
- **Tier thresholds** — Bronze/Silver/Gold boundary points
- **Non-stackable benefits** — single highest discount enforced
- **Inventory holds** — 30-minute reservations, confirm/cancel rollback
- **Low-stock alerts** — threshold detection, logging
- **SLA escalation** — 15-min unacknowledged auto-escalation to Moderator,
  24-hr unresolved escalation to L2
- **Appeal transitions** — forward-only state machine (SUBMITTED -> APPROVED/REJECTED)
- **Upload validation** — photos/PDF, 10 MB limit, 5 files max
- **Encryption/masking** — AES-GCM round-trip, phone masking `***-***-1234`
- **Deletion cooling-off** — 30-day window, cancel, process
- **Idempotent flows** — reservation, fulfillment, acknowledgment deduplication
- **Audit retention** — 2-year policy, purge job

---

## Architecture

### Layered Backend

```
com.demo.app
 ├── api/                          HTTP boundary
 │   ├── controller/               REST endpoints, @PreAuthorize
 │   ├── dto/                      Request/response records
 │   └── mapper/                   DTO <-> domain conversion
 ├── application/service/          Use-case orchestration
 ├── domain/                       Pure business rules
 │   ├── enums/                    Role, Status, Type enums
 │   └── model/                    Framework-free POJOs
 ├── persistence/                  Data access
 │   ├── entity/                   JPA entities
 │   └── repository/               Spring Data interfaces
 ├── security/                     JWT, filters, RBAC config
 └── infrastructure/               Cross-cutting
     ├── audit/                    AOP audit logging
     ├── encryption/               AES-GCM field encryption
     ├── ratelimit/                Bucket4j + login attempt tracking
     └── scheduler/                SLA checks, reservation expiry, purge
```

### Layered Frontend

```
frontend/src
 ├── api/                          Axios clients per domain
 ├── state/                        Zustand stores
 ├── components/
 │   ├── ui/                       Badge, Button, Card, Input, Modal, Table
 │   └── layout/                   AppLayout, ProtectedRoute
 ├── features/                     Domain hooks (useAuth, useProducts, ...)
 └── pages/                        Route-level components
```

### Database (15 Flyway Migrations)

```
V1  Base tables:        app_user, category, product, product_order
V2  Seed data:          6 users, 4 categories, 6 products, 3 orders
V3  Member system:      member_tier (spend bands), benefit_package,
                        benefit_item (scope + exclusion_group),
                        member_profile (total_spend), spend_ledger,
                        benefit_issuance_ledger, benefit_redemption_ledger
V4  Discovery:          listing (GIN indexes, neighborhood, lat/lng,
                        available_from/to for geo+date filtering)
V5  Warehouse:          warehouse, inventory_item (generated column),
                        inventory_movement, stock_reservation
V6  Incidents:          incident, incident_comment,
                        incident_escalation_log, appeal
V7  Fulfillment:        fulfillment, fulfillment_step
V8  Infrastructure:     account_deletion_request, audit_log,
                        login_attempt, risk_event, risk_score
V9  Refinements:        listing geo columns, tier spend bands,
                        benefit scoping, appeal_evidence uploads,
                        risk metrics (complaint/incident/appeal counts)
V10 Tier correction:    strict 0-499, 500-1499, 1500+ spend bands,
                        listing price/sqft/layout, incident address fields
V11 Heritage crafts:    marketplace seed data (pottery, textiles,
                        woodwork, metalwork)
V12 Benefit scoping:    benefit category_id, seller_id, valid_from/to;
                        incident closure_code; listing weekly_views
V13 Order accounting:   tender_type, refund, reconciliation fields;
                        STOCKTAKE/INBOUND/OUTBOUND movement types
V14 Low-stock strict:   enforce minimum threshold >= 5 system-wide
V15 Appeal evidence:    DB trigger enforcing max 5 files per appeal
```

---

## Sequence Diagrams

### Login with Rate Limiting and Lockout

```
Client              RateLimitFilter    AuthController    LoginAttemptService    JwtService
  |--- POST /auth/login --->|               |                   |                  |
  |                          |--check bucket-->                  |                  |
  |                          |<--allow---------|                 |                  |
  |                          |---------->| login()              |                  |
  |                          |           |---isLockedOut(user)-->|                  |
  |                          |           |<--false--------------|                  |
  |                          |           |---authenticate()---->|                  |
  |                          |           |---recordAttempt(ok)-->|                  |
  |                          |           |---generateToken()---------------------->|
  |                          |           |<--------------------------token---------|
  |<---200 {token}-----------|           |                      |                  |
```

### Stock Reservation with 30-Minute Hold

```
Client              ReservationService         InventoryItem           MovementLog
  |--- POST /reservations -->|                      |                      |
  |   {itemId, qty, key}     |                      |                      |
  |                          |--check idempotency-->|                      |
  |                          |--check available---->|                      |
  |                          |--reserve(+qty)------>| qtyReserved += qty   |
  |                          |--record movement---------------------------->|
  |                          |--set expiresAt=now+30m                      |
  |<--200 {status:HELD}------|                      |                      |
  |                          |                      |                      |
  |--- POST /confirm ------->|                      |                      |
  |                          |--deduct onHand------>| qtyOnHand -= qty     |
  |                          |--release reserved--->| qtyReserved -= qty   |
  |                          |--record movement---------------------------->|
  |<--200 {status:CONFIRMED}-|                      |                      |
```

### Incident SLA Escalation

```
ScheduledTasks (every 60s)      IncidentEscalationService        IncidentRepository
       |---checkAndEscalate()-------->|                                |
       |                              |--findUnacknowledgedPastSla()-->|
       |                              |<--[incident A: 20min overdue]--|
       |                              |--escalate(A, level=1)          |
       |                              |--autoAssign(MODERATOR)         |
       |                              |--logEscalation()               |
       |                              |                                |
       |                              |--findUnresolvedPastSla()------>|
       |                              |<--[incident B: 25hr overdue]---|
       |                              |--escalate(B, level=2)          |
       |<-----------------------------|                                |
```

### Account Deletion Cooling-Off

```
Day 0              Day 1-29             Day 30 (02:00 UTC cron)
  |                   |                      |
  POST /request       |                      ScheduledTasks
  status=PENDING      |                      |--processExpired()
  coolingOff=+30d     |                      |--find PENDING where coolingOff < now
  |                   |                      |--disable user account
  |  POST /cancel     |                      |--status=PROCESSED
  |  (optional)       |                      |
  |  status=CANCELLED |                      |
```

---

## Sample API Requests and Responses

### Authentication

```bash
# Login
curl -sk -X POST https://localhost:8443/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password123"}'

# 200 OK
{"token":"eyJhbG...","username":"admin","role":"ADMINISTRATOR"}
```

### Listing Search (with geo/date filtering)

```bash
# Keyword search (public)
curl -sk "https://localhost:8443/api/listings/search?q=keyboard"

# Neighborhood filter
curl -sk "https://localhost:8443/api/listings/search?neighborhood=Downtown"

# Distance sort (closest to lat/lng first)
curl -sk "https://localhost:8443/api/listings/search?lat=40.73&lng=-73.99"

# Date window filter
curl -sk "https://localhost:8443/api/listings/search?availableAfter=2026-03-01&availableBefore=2026-06-30"

# Combined
curl -sk "https://localhost:8443/api/listings/search?q=headphones&neighborhood=Downtown&lat=40.71&lng=-74.01&availableAfter=2026-01-01"

# 200 OK
[{"id":1,"title":"Wireless Headphones - Premium ANC",
  "neighborhood":"Downtown","latitude":40.7128,"longitude":-74.006,
  "availableFrom":"2026-01-01","availableTo":"2026-12-31",...}]
```

### Stock Reservation

```bash
TOKEN="eyJhbG..."

# Reserve 5 units
curl -sk -X POST https://localhost:8443/api/reservations \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"inventoryItemId":1,"quantity":5,"idempotencyKey":"res-001-1234"}'

# 200 OK
{"id":1,"inventoryItemId":1,"userId":4,"quantity":5,"status":"HELD",
 "idempotencyKey":"res-001-1234","expiresAt":"2026-04-09T09:30:00",
 "createdAt":"2026-04-09T09:00:00","confirmedAt":null,"cancelledAt":null}

# Confirm
curl -sk -X POST https://localhost:8443/api/reservations/1/confirm \
  -H "Authorization: Bearer $TOKEN"

# 200 OK
{"id":1,...,"status":"CONFIRMED","confirmedAt":"2026-04-09T09:05:00"}
```

### Create Incident

```bash
curl -sk -X POST https://localhost:8443/api/incidents \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"incidentType":"ORDER_ISSUE","severity":"HIGH",
       "title":"Wrong item shipped","description":"Received keyboard instead of headphones.\n\n[Location Notes: Warehouse B, Dock 3]"}'

# 200 OK
{"id":3,"reporterId":2,"assigneeId":null,"incidentType":"ORDER_ISSUE",
 "severity":"HIGH","title":"Wrong item shipped","status":"OPEN",
 "slaAckDeadline":"2026-04-09T09:15:00","slaResolveDeadline":"2026-04-10T09:00:00",
 "escalationLevel":0,"createdAt":"2026-04-09T09:00:00","acknowledgedAt":null,"resolvedAt":null}
```

### Error Contract

All errors return a consistent JSON envelope with precise HTTP status codes:

```json
// 400 Bad Request — malformed input
{"error": "Username already taken: admin"}

// 401 Unauthorized — invalid/missing credentials
{"error": "Authentication failed"}

// 403 Forbidden — RBAC or ownership violation
{"error": "Access denied"}
{"error": "You do not have access to this resource"}
{"error": "Cannot modify another user's reservation"}

// 404 Not Found — resource does not exist
{"error": "Reservation not found: 99"}
{"error": "Tier not found: 42"}

// 409 Conflict — state/business rule violation
{"error": "A pending deletion request already exists for this account."}
{"error": "Insufficient stock. Available: 3, requested: 10"}
{"error": "Cannot redeem: another benefit in exclusion group 'DISCOUNT_GROUP' is already applied"}
{"error": "Reservation cannot be confirmed. Current status: EXPIRED"}

// 429 Too Many Requests — rate limited (configurable via app.rate-limit.requests-per-minute)
{"error": "Rate limit exceeded. Max 60 requests per minute."}
```

Validation errors include field details:

```json
// 400 Validation Error
{"error": "Validation failed", "fields": {"title": "must not be blank"}}
```

---

## Verification Checklist

Run these after `docker compose up --build` to confirm all flows work:

```bash
# 1. Public listing search (no auth)
curl -sk https://localhost:8443/api/listings/search?q=headphones | jq length
# Expected: 1

# 2. Login and get profile with tier
TOKEN=$(curl -sk https://localhost:8443/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password123"}' | jq -r .token)
curl -sk https://localhost:8443/api/members/me \
  -H "Authorization: Bearer $TOKEN" | jq '{tier: .tierName, points: .points}'
# Expected: {"tier":"Gold","points":25000}

# 3. Tier benefit packages
curl -sk https://localhost:8443/api/benefits/packages/tier/3 \
  -H "Authorization: Bearer $TOKEN" | jq '.[0].name'
# Expected: "Gold Elite"

# 4. Inventory and low-stock check
curl -sk https://localhost:8443/api/warehouses \
  -H "Authorization: Bearer $TOKEN" | jq length
# Expected: 3

# 5. Create and acknowledge incident
INC=$(curl -s -X POST https://localhost:8443/api/incidents \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"incidentType":"OTHER","severity":"NORMAL","title":"Test","description":"Verification"}' \
  | jq -r .id)
curl -s -X POST "https://localhost:8443/api/incidents/$INC/acknowledge" \
  -H "Authorization: Bearer $TOKEN" | jq .status
# Expected: "ACKNOWLEDGED"

# 6. Create and review appeal
APPEAL=$(curl -s -X POST https://localhost:8443/api/appeals \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"relatedEntityType":"PRODUCT","relatedEntityId":5,"reason":"Should be approved"}' \
  | jq -r .id)
curl -s -X POST "https://localhost:8443/api/appeals/$APPEAL/review" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"APPROVED","reviewNotes":"Looks good"}' | jq .status
# Expected: "APPROVED"

# 7. Risk analytics (local-only, no external APIs)
curl -s -X POST https://localhost:8443/api/risk/compute/2 \
  -H "Authorization: Bearer $TOKEN" | jq '{score: .score, computedAt: .computedAt}'
# Expected: {"score": <number>, "computedAt": "<timestamp>"}

# 8. RBAC enforcement
MEMBER_TOKEN=$(curl -sk https://localhost:8443/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"member","password":"password123"}' | jq -r .token)
curl -s -o /dev/null -w "%{http_code}" https://localhost:8443/api/users \
  -H "Authorization: Bearer $MEMBER_TOKEN"
# Expected: 403

# 9. TLS proxy works
curl -sk https://localhost:8443/api/categories | jq length
# Expected: 4

# 10. No external calls — verify offline
docker exec demo-backend wget -q --timeout=2 http://example.com -O /dev/null 2>&1 || echo "OFFLINE OK"
# Expected: OFFLINE OK (or timeout — no external deps required)
```

---

## Offline-Only Constraints

This project is designed for fully air-gapped operation:

- **No external authentication** — JWT signed locally with HMAC-SHA
- **No external APIs** — risk analytics computed from on-prem historical data
- **No cloud services** — all storage is local PostgreSQL
- **No CDN or remote assets** — frontend is a static SPA served locally
- **No third-party messaging** — no email, SMS, or push notifications
- **No payment gateways** — no transaction processing
- **No map/geocoding services** — location is free-text notes only
- **TLS is self-signed** — generated at proxy build time, no external CA

Docker images are pulled once at build time. After that, `docker compose up`
works fully offline.

---

## Code Quality Gates

| Gate                        | Tool                 | Threshold | Enforcement        |
|-----------------------------|----------------------|-----------|--------------------|
| Backend line coverage       | JaCoCo 0.8.12        | >= 50%    | `mvn test` fails   |
| Frontend line coverage      | Vitest + v8          | >= 90%    | `vitest run` fails  |
| Frontend branch coverage    | Vitest + v8          | >= 90%    | `vitest run` fails  |
| Frontend function coverage  | Vitest + v8          | >= 90%    | `vitest run` fails  |
| API test pass rate          | pytest               | 100%      | Non-zero exit       |
| Schema consistency          | Flyway + ddl-auto=validate | Strict | App won't start  |
| Java compilation            | Maven + javac 21     | 0 errors  | Build fails         |
| TypeScript                  | tsc --strict         | 0 errors  | Build fails         |

---

## Project Structure

```
.
├── docker-compose.yml                 Single-command orchestration
├── run_tests.sh                       Test runner (all suites)
├── .gitignore                         Java, Node, Python, IDE, Docker
├── README.md                          This file
├── backend/
│   ├── Dockerfile                     Multi-stage Maven + JRE 21
│   ├── pom.xml                        Dependencies + JaCoCo
│   └── src/main/
│       ├── java/com/demo/app/         178 Java source files
│       └── resources/
│           ├── application.yml         All config (no .env)
│           └── db/migration/           14 Flyway SQL migrations
├── frontend/
│   ├── Dockerfile                     Multi-stage Node 20
│   ├── package.json                   React, Axios, Zustand, Vitest
│   ├── vitest.config.ts               Test + coverage config
│   └── src/                           52 TypeScript/TSX files
├── proxy/
│   ├── Dockerfile                     Nginx + self-signed TLS
│   └── nginx.conf                     Reverse proxy config
├── unit_tests/
│   ├── backend/
│   │   ├── java/com/demo/app/         18 JUnit test files
│   │   └── resources/                 H2 test configuration
│   └── frontend/                      16 Vitest test files
└── API_tests/
    ├── conftest.py                    Shared fixtures
    ├── requirements.txt               pytest, requests
    ├── Dockerfile                     Python 3.12 test runner
    └── test_*.py                      10 integration test files
```

# API Specification

## 1. Conventions
Base path:
- `/api`

Auth:
- Bearer token in `Authorization: Bearer <token>` for protected endpoints.

Content types:
- Request/response: `application/json`
- Evidence upload: `multipart/form-data`

Common status codes:
- `200` Success
- `201` Created (where applicable)
- `400` Validation or bad request
- `401` Unauthenticated
- `403` Forbidden
- `404` Not found
- `409` Conflict
- `429` Rate limit exceeded

## 2. Authentication
### POST /auth/login
Authenticates a user and returns JWT token payload.

Request body:
```json
{
  "username": "admin",
  "password": "password123"
}
```

Response body:
```json
{
  "token": "<jwt>",
  "username": "admin",
  "role": "ADMINISTRATOR"
}
```

### POST /auth/register
Registers a user and returns an auth token.

## 3. Listing Discovery
### GET /listings
Returns published listings.

### GET /listings/slug/{slug}
Returns listing by slug.

### GET /listings/search
Supported query params:
- `q`, `neighborhood`
- `lat`, `lng`, `radiusMiles`
- `availableAfter`, `availableBefore`
- `minPrice`, `maxPrice`, `minSqft`, `maxSqft`, `layout`

## 4. Members and Benefits
### GET /tiers
Returns membership tiers and spend boundaries.

### GET /members/me
Returns current member profile.

### PUT /members/me/phone
Updates member phone data.

### GET /members/me/spend/history
Returns spend ledger entries.

### GET /benefits/packages/tier/{tierId}
Returns active benefit packages for tier.

### GET /benefits/items/package/{packageId}
Returns benefit items in package.

### POST /benefits/redeem
Redeems a benefit within scope and transition constraints.

## 5. Inventory, Reservation, Fulfillment
### GET /warehouses
Returns warehouse list.

### GET /inventory/product/{productId}
Returns inventory by product.

### GET /inventory/low-stock
Returns low-stock inventory records.

### POST /inventory/inbound
Records inbound movement.

### POST /inventory/outbound
Records outbound movement.

### POST /inventory/stocktake
Records stocktake counted quantity.

### POST /reservations
Creates reservation hold (idempotent by key).

Request body:
```json
{
  "inventoryItemId": 1,
  "quantity": 2,
  "idempotencyKey": "resv-abc-123"
}
```

### POST /reservations/{id}/confirm
Confirms held reservation.

### POST /reservations/{id}/cancel
Cancels held reservation.

### GET /reservations/my
Returns current user's reservations.

### POST /fulfillments
Creates fulfillment record (idempotent by key).

### POST /fulfillments/{id}/advance
Advances fulfillment step (`PICK`, `PACK`, `SHIP`, `DELIVER`) under allowed transition rules.

### POST /fulfillments/{id}/cancel
Cancels fulfillment in cancellable states.

## 6. Incidents and Appeals
### POST /incidents
Creates incident ticket and SLA deadlines.

### GET /incidents/my
Returns reporter-owned incidents.

### POST /incidents/{id}/acknowledge
Moderator/Admin acknowledges incident.

### PATCH /incidents/{id}/status
Updates incident status. `closureCode` required when resolving.

Request body example:
```json
{
  "status": "RESOLVED",
  "closureCode": "FIXED"
}
```

### POST /incidents/{id}/comments
Adds incident comment.

### POST /appeals
Creates appeal record.

### GET /appeals/my
Returns current user's appeals.

### POST /appeals/{id}/review
Moderator/Admin reviews appeal.

### POST /appeals/{id}/evidence
Uploads evidence file (photo/PDF constraints).

## 7. Risk and Audit
### POST /risk/events
Records risk event.

### POST /risk/compute/{userId}
Computes risk score for target user (admin).

### GET /risk/high-risk
Lists high-risk users above threshold.

### GET /audit/{entityType}/{entityId}
Returns audit trail for entity.

## 8. Account Deletion
### POST /account-deletion/request
Creates deletion request with cooling-off period.

### POST /account-deletion/{id}/cancel
Cancels pending deletion request.

### GET /account-deletion/status
Returns current deletion request state for authenticated user.

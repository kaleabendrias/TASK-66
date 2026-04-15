# Test Coverage Audit

## Backend Endpoint Inventory

Total endpoints discovered: **86**.
Definition used: unique `METHOD + resolved PATH` from Spring controller mappings, including class-level `@RequestMapping` prefixes.

| Endpoint | Source |
|---|---|
| POST /api/account-deletion/request | backend/src/main/java/com/demo/app/api/controller/AccountDeletionController.java:21 |
| POST /api/account-deletion/{id}/cancel | backend/src/main/java/com/demo/app/api/controller/AccountDeletionController.java:33 |
| GET /api/account-deletion/status | backend/src/main/java/com/demo/app/api/controller/AccountDeletionController.java:44 |
| GET /api/appeals/my | backend/src/main/java/com/demo/app/api/controller/AppealController.java:108 |
| GET /api/appeals/{id} | backend/src/main/java/com/demo/app/api/controller/AppealController.java:117 |
| POST /api/appeals | backend/src/main/java/com/demo/app/api/controller/AppealController.java:126 |
| POST /api/appeals/{id}/review | backend/src/main/java/com/demo/app/api/controller/AppealController.java:139 |
| POST /api/appeals/{id}/evidence | backend/src/main/java/com/demo/app/api/controller/AppealController.java:148 |
| GET /api/appeals/{id}/evidence | backend/src/main/java/com/demo/app/api/controller/AppealController.java:252 |
| GET /api/appeals | backend/src/main/java/com/demo/app/api/controller/AppealController.java:99 |
| GET /api/audit/{entityType}/{entityId} | backend/src/main/java/com/demo/app/api/controller/AuditController.java:322 |
| POST /api/auth/login | backend/src/main/java/com/demo/app/api/controller/AuthController.java:349 |
| POST /api/auth/register | backend/src/main/java/com/demo/app/api/controller/AuthController.java:356 |
| GET /api/benefits/packages/tier/{tierId} | backend/src/main/java/com/demo/app/api/controller/BenefitController.java:395 |
| GET /api/benefits/items/package/{packageId} | backend/src/main/java/com/demo/app/api/controller/BenefitController.java:403 |
| POST /api/benefits/issue | backend/src/main/java/com/demo/app/api/controller/BenefitController.java:411 |
| POST /api/benefits/redeem | backend/src/main/java/com/demo/app/api/controller/BenefitController.java:423 |
| GET /api/categories | backend/src/main/java/com/demo/app/api/controller/CategoryController.java:486 |
| GET /api/categories/{id} | backend/src/main/java/com/demo/app/api/controller/CategoryController.java:494 |
| POST /api/categories | backend/src/main/java/com/demo/app/api/controller/CategoryController.java:500 |
| PUT /api/categories/{id} | backend/src/main/java/com/demo/app/api/controller/CategoryController.java:508 |
| DELETE /api/categories/{id} | backend/src/main/java/com/demo/app/api/controller/CategoryController.java:516 |
| POST /api/fulfillments | backend/src/main/java/com/demo/app/api/controller/FulfillmentController.java:556 |
| POST /api/fulfillments/{id}/advance | backend/src/main/java/com/demo/app/api/controller/FulfillmentController.java:564 |
| POST /api/fulfillments/{id}/cancel | backend/src/main/java/com/demo/app/api/controller/FulfillmentController.java:580 |
| GET /api/fulfillments/order/{orderId} | backend/src/main/java/com/demo/app/api/controller/FulfillmentController.java:590 |
| GET /api/fulfillments/{id}/steps | backend/src/main/java/com/demo/app/api/controller/FulfillmentController.java:602 |
| GET /api/incidents | backend/src/main/java/com/demo/app/api/controller/IncidentController.java:763 |
| GET /api/incidents/my | backend/src/main/java/com/demo/app/api/controller/IncidentController.java:772 |
| GET /api/incidents/{id} | backend/src/main/java/com/demo/app/api/controller/IncidentController.java:781 |
| POST /api/incidents | backend/src/main/java/com/demo/app/api/controller/IncidentController.java:787 |
| POST /api/incidents/{id}/acknowledge | backend/src/main/java/com/demo/app/api/controller/IncidentController.java:804 |
| PATCH /api/incidents/{id}/status | backend/src/main/java/com/demo/app/api/controller/IncidentController.java:812 |
| POST /api/incidents/{id}/comments | backend/src/main/java/com/demo/app/api/controller/IncidentController.java:821 |
| GET /api/incidents/{id}/comments | backend/src/main/java/com/demo/app/api/controller/IncidentController.java:829 |
| GET /api/incidents/{id}/escalations | backend/src/main/java/com/demo/app/api/controller/IncidentController.java:838 |
| POST /api/inventory/stocktake | backend/src/main/java/com/demo/app/api/controller/InventoryController.java:1010 |
| GET /api/inventory/product/{productId} | backend/src/main/java/com/demo/app/api/controller/InventoryController.java:933 |
| GET /api/inventory/low-stock | backend/src/main/java/com/demo/app/api/controller/InventoryController.java:950 |
| POST /api/inventory/adjust | backend/src/main/java/com/demo/app/api/controller/InventoryController.java:968 |
| POST /api/inventory/inbound | backend/src/main/java/com/demo/app/api/controller/InventoryController.java:986 |
| POST /api/inventory/outbound | backend/src/main/java/com/demo/app/api/controller/InventoryController.java:998 |
| GET /api/listings | backend/src/main/java/com/demo/app/api/controller/ListingController.java:1101 |
| GET /api/listings/slug/{slug} | backend/src/main/java/com/demo/app/api/controller/ListingController.java:1109 |
| GET /api/listings/search | backend/src/main/java/com/demo/app/api/controller/ListingController.java:1116 |
| POST /api/listings | backend/src/main/java/com/demo/app/api/controller/ListingController.java:1145 |
| PUT /api/listings/{id} | backend/src/main/java/com/demo/app/api/controller/ListingController.java:1161 |
| POST /api/listings/{id}/publish | backend/src/main/java/com/demo/app/api/controller/ListingController.java:1188 |
| POST /api/listings/{id}/archive | backend/src/main/java/com/demo/app/api/controller/ListingController.java:1195 |
| GET /api/members/me | backend/src/main/java/com/demo/app/api/controller/MemberProfileController.java:1266 |
| PUT /api/members/me/phone | backend/src/main/java/com/demo/app/api/controller/MemberProfileController.java:1274 |
| POST /api/members/me/spend | backend/src/main/java/com/demo/app/api/controller/MemberProfileController.java:1282 |
| GET /api/members/me/spend/history | backend/src/main/java/com/demo/app/api/controller/MemberProfileController.java:1296 |
| GET /api/members/{userId} | backend/src/main/java/com/demo/app/api/controller/MemberProfileController.java:1305 |
| GET /api/tiers | backend/src/main/java/com/demo/app/api/controller/MemberTierController.java:1361 |
| GET /api/tiers/{id} | backend/src/main/java/com/demo/app/api/controller/MemberTierController.java:1369 |
| GET /api/orders | backend/src/main/java/com/demo/app/api/controller/OrderController.java:1413 |
| GET /api/orders/{id} | backend/src/main/java/com/demo/app/api/controller/OrderController.java:1422 |
| GET /api/orders/buyer/{buyerId} | backend/src/main/java/com/demo/app/api/controller/OrderController.java:1429 |
| POST /api/orders | backend/src/main/java/com/demo/app/api/controller/OrderController.java:1438 |
| PATCH /api/orders/{id}/status | backend/src/main/java/com/demo/app/api/controller/OrderController.java:1462 |
| GET /api/products | backend/src/main/java/com/demo/app/api/controller/ProductController.java:1549 |
| GET /api/products/{id} | backend/src/main/java/com/demo/app/api/controller/ProductController.java:1557 |
| GET /api/products/seller/{sellerId} | backend/src/main/java/com/demo/app/api/controller/ProductController.java:1563 |
| GET /api/products/status/{status} | backend/src/main/java/com/demo/app/api/controller/ProductController.java:1571 |
| POST /api/products | backend/src/main/java/com/demo/app/api/controller/ProductController.java:1579 |
| PUT /api/products/{id} | backend/src/main/java/com/demo/app/api/controller/ProductController.java:1590 |
| DELETE /api/products/{id} | backend/src/main/java/com/demo/app/api/controller/ProductController.java:1605 |
| POST /api/reservations | backend/src/main/java/com/demo/app/api/controller/ReservationController.java:1654 |
| POST /api/reservations/{id}/confirm | backend/src/main/java/com/demo/app/api/controller/ReservationController.java:1663 |
| POST /api/reservations/{id}/cancel | backend/src/main/java/com/demo/app/api/controller/ReservationController.java:1670 |
| GET /api/reservations/my | backend/src/main/java/com/demo/app/api/controller/ReservationController.java:1677 |
| POST /api/risk/compute/{userId} | backend/src/main/java/com/demo/app/api/controller/RiskAnalyticsController.java:1735 |
| GET /api/risk/score/{userId} | backend/src/main/java/com/demo/app/api/controller/RiskAnalyticsController.java:1741 |
| GET /api/risk/high-risk | backend/src/main/java/com/demo/app/api/controller/RiskAnalyticsController.java:1751 |
| GET /api/risk/events/{userId} | backend/src/main/java/com/demo/app/api/controller/RiskAnalyticsController.java:1758 |
| POST /api/risk/events | backend/src/main/java/com/demo/app/api/controller/RiskAnalyticsController.java:1764 |
| GET /api/users | backend/src/main/java/com/demo/app/api/controller/UserController.java:1795 |
| GET /api/users/{id} | backend/src/main/java/com/demo/app/api/controller/UserController.java:1803 |
| GET /api/users/me | backend/src/main/java/com/demo/app/api/controller/UserController.java:1808 |
| PUT /api/users/{id} | backend/src/main/java/com/demo/app/api/controller/UserController.java:1814 |
| DELETE /api/users/{id} | backend/src/main/java/com/demo/app/api/controller/UserController.java:1820 |
| GET /api/warehouses | backend/src/main/java/com/demo/app/api/controller/WarehouseController.java:1845 |
| GET /api/warehouses/{id} | backend/src/main/java/com/demo/app/api/controller/WarehouseController.java:1853 |
| POST /api/warehouses | backend/src/main/java/com/demo/app/api/controller/WarehouseController.java:1858 |
| PUT /api/warehouses/{id} | backend/src/main/java/com/demo/app/api/controller/WarehouseController.java:1870 |

## API Test Mapping Table

Coverage rule applied: endpoint is covered only when a test issues an HTTP request to the matching method/path.

| Endpoint | Covered | Test Type | Test Files | Evidence |
|---|---|---|---|---|
| POST /api/account-deletion/request | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/AccountDeletionApiTest.java | API_tests/src/test/java/com/demo/apitest/AccountDeletionApiTest.java:102 (unauthenticatedCannotRequestDeletion) |
| POST /api/account-deletion/{id}/cancel | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/AccountDeletionApiTest.java | API_tests/src/test/java/com/demo/apitest/AccountDeletionApiTest.java:73 (cancelDeletionRequest) |
| GET /api/account-deletion/status | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/AccountDeletionApiTest.java | API_tests/src/test/java/com/demo/apitest/AccountDeletionApiTest.java:109 (unauthenticatedCannotGetDeletionStatus) |
| GET /api/appeals/my | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/AppealApiTest.java | API_tests/src/test/java/com/demo/apitest/AppealApiTest.java:91 (getMyAppeals) |
| GET /api/appeals/{id} | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/AppealApiTest.java | API_tests/src/test/java/com/demo/apitest/AppealApiTest.java:101 (getAppealById) |
| POST /api/appeals | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/AppealApiTest.java | API_tests/src/test/java/com/demo/apitest/AppealApiTest.java:215 (createAppealMissingReasonRejected) |
| POST /api/appeals/{id}/review | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/AppealApiTest.java | API_tests/src/test/java/com/demo/apitest/AppealApiTest.java:117 (reviewAppeal) |
| POST /api/appeals/{id}/evidence | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/AppealApiTest.java | API_tests/src/test/java/com/demo/apitest/AppealApiTest.java:135 (uploadEvidence) |
| GET /api/appeals/{id}/evidence | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/AppealApiTest.java | API_tests/src/test/java/com/demo/apitest/AppealApiTest.java:182 (getEvidenceList) |
| GET /api/appeals | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/AppealApiTest.java | API_tests/src/test/java/com/demo/apitest/AppealApiTest.java:81 (getAllAppeals) |
| GET /api/audit/{entityType}/{entityId} | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/RbacApiTest.java | API_tests/src/test/java/com/demo/apitest/RbacApiTest.java:171 (adminCanGetAuditTrail) |
| POST /api/auth/login | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/AuthApiTest.java | API_tests/src/test/java/com/demo/apitest/AuthApiTest.java:35 (loginSuccess) |
| POST /api/auth/register | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/AccountDeletionApiTest.java | API_tests/src/test/java/com/demo/apitest/AccountDeletionApiTest.java:33 (unknown) |
| GET /api/benefits/packages/tier/{tierId} | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/BenefitApiTest.java | API_tests/src/test/java/com/demo/apitest/BenefitApiTest.java:147 (redeemBenefit) |
| GET /api/benefits/items/package/{packageId} | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/BenefitApiTest.java | API_tests/src/test/java/com/demo/apitest/BenefitApiTest.java:155 (redeemBenefit) |
| POST /api/benefits/issue | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/BenefitApiTest.java | API_tests/src/test/java/com/demo/apitest/BenefitApiTest.java:107 (issueBenefitWithValidOrderReference) |
| POST /api/benefits/redeem | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/BenefitApiTest.java | API_tests/src/test/java/com/demo/apitest/BenefitApiTest.java:182 (redeemBenefit) |
| GET /api/categories | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/CategoryApiTest.java | API_tests/src/test/java/com/demo/apitest/CategoryApiTest.java:31 (getAllCategories) |
| GET /api/categories/{id} | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/CategoryApiTest.java | API_tests/src/test/java/com/demo/apitest/CategoryApiTest.java:39 (getCategoryById) |
| POST /api/categories | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/CategoryApiTest.java | API_tests/src/test/java/com/demo/apitest/CategoryApiTest.java:102 (deleteCategory) |
| PUT /api/categories/{id} | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/CategoryApiTest.java | API_tests/src/test/java/com/demo/apitest/CategoryApiTest.java:88 (updateCategory) |
| DELETE /api/categories/{id} | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/CategoryApiTest.java | API_tests/src/test/java/com/demo/apitest/CategoryApiTest.java:107 (deleteCategory) |
| POST /api/fulfillments | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/FulfillmentApiTest.java | API_tests/src/test/java/com/demo/apitest/FulfillmentApiTest.java:122 (fulfillmentIdempotency) |
| POST /api/fulfillments/{id}/advance | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/FulfillmentApiTest.java | API_tests/src/test/java/com/demo/apitest/FulfillmentApiTest.java:81 (createFulfillmentAndAdvanceSteps) |
| POST /api/fulfillments/{id}/cancel | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/FulfillmentApiTest.java | API_tests/src/test/java/com/demo/apitest/FulfillmentApiTest.java:136 (cancelFulfillment) |
| GET /api/fulfillments/order/{orderId} | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/FulfillmentApiTest.java | API_tests/src/test/java/com/demo/apitest/FulfillmentApiTest.java:103 (getFulfillmentByOrderId) |
| GET /api/fulfillments/{id}/steps | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/FulfillmentApiTest.java | API_tests/src/test/java/com/demo/apitest/FulfillmentApiTest.java:92 (getFulfillmentSteps) |
| GET /api/incidents | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/IncidentApiTest.java | API_tests/src/test/java/com/demo/apitest/IncidentApiTest.java:70 (listAllIncidents) |
| GET /api/incidents/my | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/IncidentApiTest.java | API_tests/src/test/java/com/demo/apitest/IncidentApiTest.java:81 (getMyIncidents) |
| GET /api/incidents/{id} | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/IncidentApiTest.java | API_tests/src/test/java/com/demo/apitest/IncidentApiTest.java:193 (userCannotViewOtherUsersIncident) |
| POST /api/incidents | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/IncidentApiTest.java | API_tests/src/test/java/com/demo/apitest/IncidentApiTest.java:42 (setup) |
| POST /api/incidents/{id}/acknowledge | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/IncidentApiTest.java | API_tests/src/test/java/com/demo/apitest/IncidentApiTest.java:101 (acknowledgeIncident) |
| PATCH /api/incidents/{id}/status | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/IncidentApiTest.java | API_tests/src/test/java/com/demo/apitest/IncidentApiTest.java:122 (resolveRequiresClosureCode) |
| POST /api/incidents/{id}/comments | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/IncidentApiTest.java | API_tests/src/test/java/com/demo/apitest/IncidentApiTest.java:168 (addAndGetComment) |
| GET /api/incidents/{id}/comments | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/IncidentApiTest.java | API_tests/src/test/java/com/demo/apitest/IncidentApiTest.java:172 (addAndGetComment) |
| GET /api/incidents/{id}/escalations | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/IncidentApiTest.java | API_tests/src/test/java/com/demo/apitest/IncidentApiTest.java:183 (getEscalations) |
| POST /api/inventory/stocktake | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/InventoryApiTest.java | API_tests/src/test/java/com/demo/apitest/InventoryApiTest.java:121 (recordStocktake) |
| GET /api/inventory/product/{productId} | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/InventoryApiTest.java | API_tests/src/test/java/com/demo/apitest/InventoryApiTest.java:34 (getInventoryByProduct) |
| GET /api/inventory/low-stock | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/InventoryApiTest.java | API_tests/src/test/java/com/demo/apitest/InventoryApiTest.java:128 (memberCannotAccessLowStock) |
| POST /api/inventory/adjust | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/InventoryApiTest.java | API_tests/src/test/java/com/demo/apitest/InventoryApiTest.java:142 (memberCannotAdjustStock) |
| POST /api/inventory/inbound | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/InventoryApiTest.java | API_tests/src/test/java/com/demo/apitest/InventoryApiTest.java:84 (recordInboundMovement) |
| POST /api/inventory/outbound | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/InventoryApiTest.java | API_tests/src/test/java/com/demo/apitest/InventoryApiTest.java:107 (recordOutboundMovement) |
| GET /api/listings | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/ListingApiTest.java | API_tests/src/test/java/com/demo/apitest/ListingApiTest.java:35 (getPublishedListings) |
| GET /api/listings/slug/{slug} | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/ListingApiTest.java | API_tests/src/test/java/com/demo/apitest/ListingApiTest.java:52 (getListingBySlug) |
| GET /api/listings/search | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/ListingApiTest.java | API_tests/src/test/java/com/demo/apitest/ListingApiTest.java:62 (searchListingsKeyword) |
| POST /api/listings | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/ListingApiTest.java | API_tests/src/test/java/com/demo/apitest/ListingApiTest.java:113 (createListing) |
| PUT /api/listings/{id} | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/ListingApiTest.java | API_tests/src/test/java/com/demo/apitest/ListingApiTest.java:147 (updateListing) |
| POST /api/listings/{id}/publish | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/ListingApiTest.java | API_tests/src/test/java/com/demo/apitest/ListingApiTest.java:169 (publishListing) |
| POST /api/listings/{id}/archive | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/ListingApiTest.java | API_tests/src/test/java/com/demo/apitest/ListingApiTest.java:196 (archiveListing) |
| GET /api/members/me | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/BenefitApiTest.java | API_tests/src/test/java/com/demo/apitest/BenefitApiTest.java:142 (redeemBenefit) |
| PUT /api/members/me/phone | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/MemberApiTest.java | API_tests/src/test/java/com/demo/apitest/MemberApiTest.java:96 (updateMemberPhone) |
| POST /api/members/me/spend | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/MemberApiTest.java | API_tests/src/test/java/com/demo/apitest/MemberApiTest.java:109 (adjustMemberSpend) |
| GET /api/members/me/spend/history | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/MemberApiTest.java | API_tests/src/test/java/com/demo/apitest/MemberApiTest.java:116 (getSpendHistory) |
| GET /api/members/{userId} | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/BenefitApiTest.java | API_tests/src/test/java/com/demo/apitest/BenefitApiTest.java:142 (redeemBenefit) |
| GET /api/tiers | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/MemberApiTest.java | API_tests/src/test/java/com/demo/apitest/MemberApiTest.java:37 (getAllTiers) |
| GET /api/tiers/{id} | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/MemberApiTest.java | API_tests/src/test/java/com/demo/apitest/MemberApiTest.java:46 (getTierById) |
| GET /api/orders | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/OrderApiTest.java | API_tests/src/test/java/com/demo/apitest/OrderApiTest.java:173 (memberCannotListAllOrders) |
| GET /api/orders/{id} | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/OrderApiTest.java | API_tests/src/test/java/com/demo/apitest/OrderApiTest.java:143 (cancelOrderReleasesReservation) |
| GET /api/orders/buyer/{buyerId} | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/OrderApiTest.java | API_tests/src/test/java/com/demo/apitest/OrderApiTest.java:106 (getOrdersByBuyer) |
| POST /api/orders | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/BenefitApiTest.java | API_tests/src/test/java/com/demo/apitest/BenefitApiTest.java:170 (redeemBenefit) |
| PATCH /api/orders/{id}/status | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/OrderApiTest.java | API_tests/src/test/java/com/demo/apitest/OrderApiTest.java:124 (buyerCanCancelOwnOrder) |
| GET /api/products | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/ProductApiTest.java | API_tests/src/test/java/com/demo/apitest/ProductApiTest.java:35 (getAllProducts) |
| GET /api/products/{id} | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/ProductApiTest.java | API_tests/src/test/java/com/demo/apitest/ProductApiTest.java:133 (updateProduct) |
| GET /api/products/seller/{sellerId} | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/ProductApiTest.java | API_tests/src/test/java/com/demo/apitest/ProductApiTest.java:61 (getProductsBySellerPublic) |
| GET /api/products/status/{status} | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/ProductApiTest.java | API_tests/src/test/java/com/demo/apitest/ProductApiTest.java:69 (getProductsByStatus) |
| POST /api/products | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/ProductApiTest.java | API_tests/src/test/java/com/demo/apitest/ProductApiTest.java:107 (createProductSellerIdBoundToAuthUser) |
| PUT /api/products/{id} | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/ProductApiTest.java | API_tests/src/test/java/com/demo/apitest/ProductApiTest.java:147 (updateProduct) |
| DELETE /api/products/{id} | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/ProductApiTest.java | API_tests/src/test/java/com/demo/apitest/ProductApiTest.java:169 (deleteProduct) |
| POST /api/reservations | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/ReservationApiTest.java | API_tests/src/test/java/com/demo/apitest/ReservationApiTest.java:121 (zeroQuantityRejected) |
| POST /api/reservations/{id}/confirm | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/ReservationApiTest.java | API_tests/src/test/java/com/demo/apitest/ReservationApiTest.java:149 (userCannotConfirmOthersReservation) |
| POST /api/reservations/{id}/cancel | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/ReservationApiTest.java | API_tests/src/test/java/com/demo/apitest/ReservationApiTest.java:78 (createReservationAndCancel) |
| GET /api/reservations/my | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/RbacApiTest.java | API_tests/src/test/java/com/demo/apitest/RbacApiTest.java:117 (memberCanGetOwnReservations) |
| POST /api/risk/compute/{userId} | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/RiskAuditApiTest.java | API_tests/src/test/java/com/demo/apitest/RiskAuditApiTest.java:200 (memberCannotComputeRiskScore) |
| GET /api/risk/score/{userId} | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/RiskAuditApiTest.java | API_tests/src/test/java/com/demo/apitest/RiskAuditApiTest.java:88 (getRiskScore) |
| GET /api/risk/high-risk | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/RbacApiTest.java | API_tests/src/test/java/com/demo/apitest/RbacApiTest.java:164 (adminCanGetHighRiskUsers) |
| GET /api/risk/events/{userId} | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/RiskAuditApiTest.java | API_tests/src/test/java/com/demo/apitest/RiskAuditApiTest.java:114 (getRiskEventsByUser) |
| POST /api/risk/events | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/RiskAuditApiTest.java | API_tests/src/test/java/com/demo/apitest/RiskAuditApiTest.java:42 (setup) |
| GET /api/users | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/RbacApiTest.java | API_tests/src/test/java/com/demo/apitest/RbacApiTest.java:178 (adminCanListAllUsers) |
| GET /api/users/{id} | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/AuthApiTest.java | API_tests/src/test/java/com/demo/apitest/AuthApiTest.java:119 (getMeEndpoint) |
| GET /api/users/me | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/AuthApiTest.java | API_tests/src/test/java/com/demo/apitest/AuthApiTest.java:119 (getMeEndpoint) |
| PUT /api/users/{id} | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/UserApiTest.java | API_tests/src/test/java/com/demo/apitest/UserApiTest.java:106 (updateUser) |
| DELETE /api/users/{id} | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/UserApiTest.java | API_tests/src/test/java/com/demo/apitest/UserApiTest.java:133 (deleteUser) |
| GET /api/warehouses | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/RbacApiTest.java | API_tests/src/test/java/com/demo/apitest/RbacApiTest.java:101 (sellerCanReadWarehouses) |
| GET /api/warehouses/{id} | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/WarehouseApiTest.java | API_tests/src/test/java/com/demo/apitest/WarehouseApiTest.java:42 (getWarehouseById) |
| POST /api/warehouses | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/WarehouseApiTest.java | API_tests/src/test/java/com/demo/apitest/WarehouseApiTest.java:113 (nonAdminCannotCreateWarehouse) |
| PUT /api/warehouses/{id} | yes | true no-mock HTTP | API_tests/src/test/java/com/demo/apitest/WarehouseApiTest.java | API_tests/src/test/java/com/demo/apitest/WarehouseApiTest.java:99 (updateWarehouse) |

## Coverage Summary

- Total endpoints: **86**
- Endpoints with HTTP tests: **86**
- Endpoints with TRUE no-mock tests: **86**
- HTTP coverage: **100.00%**
- True API coverage: **100.00%**

## API Test Classification

- True No-Mock HTTP: **19 files** (`API_tests/src/test/java/com/demo/apitest/*.java`)
- HTTP with Mocking: **0 files found**
- Non-HTTP API tests: **0 files found**

## Mock Detection Rules Check

No mock/stub indicators found in API tests:
- `jest.mock`, `vi.mock`, `sinon.stub`, `@MockBean`, `Mockito`, `mock(`, `spy(`
- Evidence scope: `API_tests/src/test/java/com/demo/apitest`

Mocking is present in frontend unit tests (expected for unit scope), e.g.:
- `unit_tests/frontend/state/authStore.test.ts`
- `unit_tests/frontend/pages/IncidentsPage.test.tsx`

## Unit Test Summary

Backend unit test files found: **36** (`unit_tests/backend/java`).

Modules clearly covered:
- Controllers: auth, order, incident, appeal, listing, benefit, member profile, risk (`unit_tests/backend/java/com/demo/app/api/controller`)
- Services: auth, user, product, order, listing, inventory, warehouse, reservation, incident, appeal, risk, member profile/tier, account deletion (`unit_tests/backend/java/com/demo/app/application/service`)
- Security/rate-limit: JWT, RBAC integration, rate-limit/login attempts (`unit_tests/backend/java/com/demo/app/security`, `unit_tests/backend/java/com/demo/app/infrastructure/ratelimit`)
- Infra/audit/encryption: audit service, field/evidence encryption, masking (`unit_tests/backend/java/com/demo/app/infrastructure`)

Important modules not directly unit-tested (controller-level files absent in `unit_tests/backend/java/com/demo/app/api/controller`):
- `AccountDeletionController`, `AuditController`, `CategoryController`, `FulfillmentController`, `InventoryController`, `MemberTierController`, `ProductController`, `ReservationController`, `UserController`, `WarehouseController`
- Repository-specific tests are largely indirect via service tests; no dedicated repository test suite observed.

## API Observability Check

Result: **Moderate to Strong**.

- Strengths: API tests generally show explicit HTTP method/path, request payload construction, status assertions, and selected response assertions (examples: `API_tests/src/test/java/com/demo/apitest/OrderApiTest.java`, `API_tests/src/test/java/com/demo/apitest/AuthApiTest.java`).
- Weaknesses: several checks are status-code dominant with limited response contract assertions (examples in security/RBAC-focused tests: `API_tests/src/test/java/com/demo/apitest/SecurityApiTest.java`, `API_tests/src/test/java/com/demo/apitest/RbacApiTest.java`).

## Tests Check

- Success paths: present across all major domains (auth, orders, products, inventory, incidents, appeals, reservations, fulfillment).
- Failure cases: present (auth failure, forbidden access, not found, invalid transitions, duplicate/idempotency).
- Edge cases: partially present (idempotency and rate-limit covered; fewer deep boundary/serialization edge assertions).
- Validation: present for required-field and enum errors in API/controller tests.
- Auth/permissions: strong RBAC/security coverage in dedicated suites.
- Integration boundaries: strong HTTP integration suite against live stack (RestAssured + deployed API target).

`run_tests.sh` assessment:
- Docker-based orchestration: **PASS** (`docker run`, `docker compose up/down`)
- Local dependency requirement: **PASS** under strict rule (no package-manager install steps, no manual DB setup required).

## End-to-End Expectations

Project is fullstack and includes browser E2E tests: **5 Playwright specs** in `repo/e2e/tests`.
Assessment: E2E exists and is meaningful; API + unit depth further compensates.

## Test Coverage Score (0-100)

**91/100**

## Score Rationale

- + Full endpoint HTTP coverage (86/86)
- + API suite is true HTTP/no-mock by static evidence
- + Strong breadth across domains and RBAC/security
- - Some endpoint evidence is setup/incidental rather than dedicated endpoint-focused assertions
- - Several tests are status-centric with limited response-body contract depth
- - Missing direct controller unit tests for multiple controllers (partially offset by API tests)

## Key Gaps

- Add explicit response-schema assertions for currently status-only checks in security/RBAC suites.
- Add direct controller-focused unit tests for currently missing controllers (or document intentional reliance on API tests).
- Reduce incidental endpoint coverage dependency (setup calls) by adding dedicated tests where needed.

## Confidence & Assumptions

- Confidence: **High** for endpoint inventory and HTTP coverage mapping; **Medium** for semantic depth/sufficiency grading.
- Assumptions:
  - `API_tests` execute against real running app stack as configured in `BaseApiTest` and `run_tests.sh`.
  - Static inspection only; no runtime behavior or flaky test dynamics were evaluated.

---

# README Audit

## Project Type Detection

- Declared/inferred type: **fullstack**
- Evidence: `repo/README.md` title and opening description references Spring Boot backend + React frontend + Dockerized stack.

## README Location Check

- Required location `repo/README.md`: **PASS**

## Hard Gate Failures

- **FAIL**: Environment strictness breach in manual verification examples due host-side Python dependency (`python3 -m json.tool`, `python3 -c`) in `repo/README.md`.
  - Strict rule requires all operational steps to be Docker-contained without extra host runtime dependencies beyond Docker/Compose.

## High Priority Issues

- Manual verification commands depend on host Python utilities; this violates strict container-contained environment expectations.

## Medium Priority Issues

- Project type is not presented as a strict top-of-file explicit declaration token (e.g., `Project Type: fullstack`), though it is clearly described in prose.
- README states API endpoint count target (86) without cross-linking to a generated endpoint inventory artifact.

## Low Priority Issues

- Manual verification section is strong but could include one concise healthcheck command (`curl /api/categories`) before full scenario steps.

## Engineering Quality

- Tech stack clarity: **Strong**
- Architecture explanation: **Strong (concise and sufficient)**
- Testing instructions: **Strong** (`run_tests.sh`, suite breakdown, targets)
- Security/roles documentation: **Strong** (seeded roles and credentials)
- Workflow clarity: **Strong** (startup, stop, verify)
- Presentation quality: **Strong**

## README Verdict

**PARTIAL PASS**

Rationale: all major documentation gates are satisfied except strict environment containment in manual verification commands due host Python usage.

---

## Final Verdicts

- Test Coverage Audit Verdict: **PASS (high coverage, moderate depth gaps)**
- README Audit Verdict: **PARTIAL PASS**

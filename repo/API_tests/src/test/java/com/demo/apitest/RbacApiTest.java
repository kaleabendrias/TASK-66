package com.demo.apitest;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.hamcrest.Matchers.*;

/**
 * Role-Based Access Control tests — verifies every role against protected endpoints.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RbacApiTest extends BaseApiTest {

    private String adminToken;
    private String memberToken;
    private String sellerToken;
    private String warehouseToken;
    private String moderatorToken;

    @BeforeAll
    void setup() {
        adminToken = login("admin");
        memberToken = login("member");
        sellerToken = login("seller");
        warehouseToken = login("warehouse");
        moderatorToken = login("moderator");
    }

    // ── Public endpoints (no auth required) ──────────────────────────────

    @Test
    void publicGetProducts() {
        noAuth().when().get("/products")
                .then().statusCode(200)
                .body("$", not(empty()))
                .body("[0].id", notNullValue())
                .body("[0].name", notNullValue())
                .body("[0].price", notNullValue());
    }

    @Test
    void publicGetCategories() {
        noAuth().when().get("/categories")
                .then().statusCode(200)
                .body("$", not(empty()))
                .body("[0].id", notNullValue())
                .body("[0].name", notNullValue());
    }

    @Test
    void publicGetListings() {
        noAuth().when().get("/listings")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    // ── Protected endpoints — require auth ───────────────────────────────

    @Test
    void ordersRequireAuth() {
        noAuth().when().get("/orders")
                .then().statusCode(anyOf(is(401), is(403)));
    }

    @Test
    void memberCannotListUsers() {
        auth(memberToken).when().get("/users")
                .then().statusCode(403);
    }

    // ── Warehouse staff ──────────────────────────────────────────────────

    @Test
    void warehouseStaffCanGetLowStock() {
        auth(warehouseToken).when().get("/inventory/low-stock")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void warehouseStaffCanGetWarehouses() {
        auth(warehouseToken).when().get("/warehouses")
                .then().statusCode(200)
                .body("$", not(empty()))
                .body("[0].id", notNullValue())
                .body("[0].name", notNullValue());
    }

    @Test
    void warehouseStaffCanGetAllOrders() {
        auth(warehouseToken).when().get("/orders")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    // ── Seller ───────────────────────────────────────────────────────────

    @Test
    void sellerCanReadWarehouses() {
        auth(sellerToken).when().get("/warehouses")
                .then().statusCode(200)
                .body("$", not(empty()));
    }

    @Test
    void sellerCanGetInventoryForOwnProduct() {
        auth(sellerToken).when().get("/inventory/product/1")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    // ── Member ────────────────────────────────────────────────────────────

    @Test
    void memberCanGetOwnReservations() {
        auth(memberToken).when().get("/reservations/my")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void memberCannotAccessLowStock() {
        auth(memberToken).when().get("/inventory/low-stock")
                .then().statusCode(403);
    }

    @Test
    void memberCanGetMemberProfile() {
        auth(memberToken).when().get("/members/me")
                .then().statusCode(200)
                .body("userId", notNullValue())
                .body("tierName", notNullValue());
    }

    @Test
    void memberCanGetTiers() {
        auth(memberToken).when().get("/tiers")
                .then().statusCode(200)
                .body("$", not(empty()))
                .body("[0].name", notNullValue());
    }

    // ── Moderator ─────────────────────────────────────────────────────────

    @Test
    void moderatorCanGetIncidents() {
        auth(moderatorToken).when().get("/incidents")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void moderatorCanGetAppeals() {
        auth(moderatorToken).when().get("/appeals")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    // ── Administrator ─────────────────────────────────────────────────────

    @Test
    void adminCanGetHighRiskUsers() {
        auth(adminToken).when().get("/risk/high-risk")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void adminCanGetAuditTrail() {
        auth(adminToken).when().get("/audit/USER/1")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void adminCanListAllUsers() {
        auth(adminToken).when().get("/users")
                .then().statusCode(200)
                .body("$", not(empty()))
                .body("[0].id", notNullValue())
                .body("[0].username", notNullValue())
                .body("[0].role", notNullValue());
    }

    @Test
    void memberCannotAccessAudit() {
        auth(memberToken).when().get("/audit/USER/1")
                .then().statusCode(403);
    }
}

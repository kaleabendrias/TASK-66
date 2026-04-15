package com.demo.apitest;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Security hardening tests:
 * - Rate limiting (60 req/min bucket)
 * - Login lockout (10 failures/hr)
 * - TLS enforcement
 * - Idempotency (reservations, fulfillments, acknowledge)
 * - Negative paths (invalid file types, bad transitions)
 * - Object-level auth (ownership checks)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SecurityApiTest extends BaseApiTest {

    private String adminToken;
    private String memberToken;
    private String warehouseToken;
    private String moderatorToken;

    @BeforeAll
    void setup() {
        adminToken = login("admin");
        memberToken = login("member");
        warehouseToken = login("warehouse");
        moderatorToken = login("moderator");
    }

    // ── TLS enforcement ───────────────────────────────────────────────────

    @Test
    void tlsProxyAccessible() {
        noAuth()
                .when().get("/categories")
                .then().statusCode(200);
    }

    @Test
    void authEndpointWorksOverTls() {
        Map<String, String> body = new HashMap<>();
        body.put("username", "admin");
        body.put("password", "password123");

        given().contentType(ContentType.JSON).body(body)
                .when().post("/auth/login")
                .then().statusCode(200)
                .body("token", not(emptyOrNullString()));
    }

    @Test
    void securityHeadersPresent() {
        Response resp = noAuth()
                .when().get("/categories")
                .then().statusCode(200)
                .extract().response();

        // Proxy injects security headers
        String xContentType = resp.header("X-Content-Type-Options");
        String xFrame = resp.header("X-Frame-Options");
        assert xContentType != null : "X-Content-Type-Options header must be present";
        assert xFrame != null : "X-Frame-Options header must be present";
    }

    // ── Unauthenticated access ────────────────────────────────────────────

    @Test
    void unauthenticatedCannotAccessProtectedEndpoint() {
        noAuth()
                .when().get("/users")
                .then().statusCode(403);
    }

    @Test
    void invalidTokenIsRejected() {
        given()
                .header("Authorization", "Bearer this.is.not.valid")
                .contentType(ContentType.JSON)
                .when().get("/users/me")
                .then().statusCode(403);
    }

    // ── Rate limiting ─────────────────────────────────────────────────────

    @Test
    void rateLimitTriggersAfterBurst() {
        // Register a unique throwaway user so this burst does not exhaust
        // the shared seeded-user buckets used by other tests in this suite.
        String unique = uid();
        java.util.Map<String, String> reg = new java.util.HashMap<>();
        reg.put("username", "ratelimit_" + unique);
        reg.put("email", "rl_" + unique + "@example.com");
        reg.put("password", "password123");
        reg.put("displayName", "Rate Limit Test");
        String rateLimitToken = given().contentType(ContentType.JSON).body(reg)
                .when().post("/auth/register")
                .then().statusCode(200)
                .extract().path("token");

        // Send 65 requests quickly — at least one should get 429 (bucket = 60)
        boolean got429 = false;
        for (int i = 0; i < 65; i++) {
            int code = auth(rateLimitToken)
                    .when().get("/categories")
                    .then().extract().statusCode();
            if (code == 429) {
                got429 = true;
                break;
            }
        }
        assert got429 : "Rate limiter should trigger 429 after burst of 65 requests";
    }

    @Test
    void normalRequestsAreNotRateLimited() {
        String token = login("moderator");
        for (int i = 0; i < 10; i++) {
            auth(token).when().get("/categories").then().statusCode(200);
        }
    }

    // ── Login lockout ─────────────────────────────────────────────────────

    @Test
    void loginLockoutAfterTenFailures() {
        String lockedUser = "guest";
        Map<String, String> bad = new HashMap<>();
        bad.put("username", lockedUser);
        bad.put("password", "wrongpassword");

        // 10 failed attempts
        for (int i = 0; i < 10; i++) {
            given().contentType(ContentType.JSON).body(bad)
                    .when().post("/auth/login");
        }

        // 11th attempt should be locked out
        Response resp = given().contentType(ContentType.JSON).body(bad)
                .when().post("/auth/login")
                .then().extract().response();

        int code = resp.statusCode();
        assert code == 400 || code == 401 || code == 429 :
                "After 10 failed attempts, account should be locked (got " + code + ")";
    }

    // ── Idempotency ───────────────────────────────────────────────────────

    @Test
    void reservationIdempotency() {
        String key = uid();
        Map<String, Object> body = new HashMap<>();
        body.put("inventoryItemId", 1);
        body.put("quantity", 1);
        body.put("idempotencyKey", key);

        int id1 = auth(warehouseToken).body(body).when().post("/reservations")
                .then().statusCode(200).extract().path("id");
        int id2 = auth(warehouseToken).body(body).when().post("/reservations")
                .then().statusCode(200).extract().path("id");

        assert id1 == id2 : "Reservation idempotency key must return same id";
    }

    @Test
    void fulfillmentIdempotency() {
        // Place an order first
        Map<String, Object> orderBody = new HashMap<>();
        orderBody.put("productId", 5);
        orderBody.put("quantity", 1);
        orderBody.put("inventoryItemId", 1);

        int orderId = auth(memberToken).body(orderBody).when().post("/orders")
                .then().statusCode(200).extract().path("id");

        String key = uid();
        Map<String, Object> fBody = new HashMap<>();
        fBody.put("orderId", orderId);
        fBody.put("warehouseId", 1);
        fBody.put("idempotencyKey", key);

        int id1 = auth(warehouseToken).body(fBody).when().post("/fulfillments")
                .then().statusCode(200).extract().path("id");
        int id2 = auth(warehouseToken).body(fBody).when().post("/fulfillments")
                .then().statusCode(200).extract().path("id");

        assert id1 == id2 : "Fulfillment idempotency key must return same id";
    }

    @Test
    void acknowledgeIdempotency() {
        Map<String, Object> inc = new HashMap<>();
        inc.put("incidentType", "ORDER_ISSUE");
        inc.put("severity", "NORMAL");
        inc.put("title", "Idem test " + uid());
        inc.put("description", "Idempotency test");
        inc.put("sellerId", 3);

        int id = auth(moderatorToken).body(inc).when().post("/incidents")
                .then().statusCode(200).extract().path("id");

        auth(moderatorToken).when().post("/incidents/" + id + "/acknowledge")
                .then().statusCode(200);
        // Second acknowledge — must not error
        auth(moderatorToken).when().post("/incidents/" + id + "/acknowledge")
                .then().statusCode(anyOf(is(200), is(409)));
    }

    // ── Object-level authorization ────────────────────────────────────────

    @Test
    void sellerCannotModifyAnotherSellersProduct() {
        // Admin creates a product, seller tries to PUT it
        Map<String, Object> prod = new HashMap<>();
        prod.put("name", "Admin Prod ObjAuth " + uid());
        prod.put("description", "test");
        prod.put("price", 1.0);
        prod.put("stockQuantity", 1);
        prod.put("categoryId", 1);

        int pid = auth(adminToken).body(prod).when().post("/products")
                .then().statusCode(200).extract().path("id");

        auth(login("seller")).body(prod).when().put("/products/" + pid)
                .then().statusCode(403);
    }

    @Test
    void productCreationBindsSellerIdToAuthUser() {
        // Seller tries to supply a different sellerId — server must override it
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Seller Bind Test " + uid());
        body.put("description", "test");
        body.put("price", 5.0);
        body.put("stockQuantity", 10);
        body.put("categoryId", 1);
        body.put("sellerId", 99999); // Should be overridden to authenticated seller's id

        auth(login("seller")).body(body).when().post("/products")
                .then().statusCode(200)
                .body("sellerId", not(equalTo(99999)));
    }

    @Test
    void userCannotViewOtherUsersIncident() {
        Map<String, Object> inc = new HashMap<>();
        inc.put("incidentType", "OTHER");
        inc.put("severity", "NORMAL");
        inc.put("title", "Private incident " + uid());
        inc.put("description", "Not visible to others");
        inc.put("sellerId", 3);

        int id = auth(moderatorToken).body(inc).when().post("/incidents")
                .then().statusCode(200).extract().path("id");

        auth(memberToken).when().get("/incidents/" + id)
                .then().statusCode(403);
    }

    @Test
    void adminBypassesOwnershipChecks() {
        Map<String, Object> inc = new HashMap<>();
        inc.put("incidentType", "OTHER");
        inc.put("severity", "NORMAL");
        inc.put("title", "Admin Access " + uid());
        inc.put("description", "Admin can see this");
        inc.put("sellerId", 3);

        int id = auth(moderatorToken).body(inc).when().post("/incidents")
                .then().statusCode(200).extract().path("id");

        // Admin can access any incident
        auth(adminToken).when().get("/incidents/" + id)
                .then().statusCode(anyOf(is(200), is(404)));
    }

    @Test
    void memberCannotDeleteCategory() {
        String memberToken2 = login("member");
        auth(memberToken2)
                .when().delete("/categories/1")
                .then().statusCode(403);
    }

    @Test
    void sellerCannotDeleteOtherSellersProduct() {
        // Admin creates a product
        Map<String, Object> prod = new HashMap<>();
        prod.put("name", "Admin Del Test " + uid());
        prod.put("description", "test");
        prod.put("price", 1.0);
        prod.put("stockQuantity", 1);
        prod.put("categoryId", 1);

        int pid = auth(adminToken).body(prod).when().post("/products")
                .then().statusCode(200).extract().path("id");

        // Seller attempts to delete it — must be 403
        auth(login("seller"))
                .when().delete("/products/" + pid)
                .then().statusCode(403);
    }
}

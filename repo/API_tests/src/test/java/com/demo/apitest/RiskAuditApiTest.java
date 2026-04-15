package com.demo.apitest;

import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;

/**
 * Tests: POST /risk/events, POST /risk/compute/{userId},
 *        GET /risk/high-risk, GET /risk/score/{userId},
 *        GET /risk/events/{userId}, GET /audit/{entityType}/{entityId}
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RiskAuditApiTest extends BaseApiTest {

    private String adminToken;
    private String moderatorToken;
    private String memberToken;

    @BeforeAll
    void setup() {
        adminToken = login("admin");
        moderatorToken = login("moderator");
        memberToken = login("member");
    }

    private void recordRiskEvent(String token, int userId, String eventType) {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        body.put("eventType", eventType);
        body.put("severity", "MEDIUM");
        body.put("details", Map.of("source", "integration-test"));

        auth(token)
                .body(body)
                .when().post("/risk/events")
                .then().statusCode(200);
    }

    @Test
    void recordRiskEventModeratorAllowed() {
        recordRiskEvent(moderatorToken, 3, "MISSED_PICKUP_CHECKIN");
    }

    @Test
    void recordRiskEventAdminAllowed() {
        recordRiskEvent(adminToken, 3, "BUDDY_PUNCHING");
    }

    @Test
    void memberCannotRecordRiskEvent() {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", 3);
        body.put("eventType", "MISIDENTIFICATION");
        body.put("severity", "LOW");

        auth(memberToken)
                .body(body)
                .when().post("/risk/events")
                .then().statusCode(403);
    }

    @Test
    void computeRiskScore() {
        // Record events first so there's data to compute from
        recordRiskEvent(adminToken, 3, "MISSED_PICKUP_CHECKIN");

        // Seller user has id=3
        auth(adminToken)
                .when().post("/risk/compute/3")
                .then().statusCode(200)
                .body("score", notNullValue());
    }

    @Test
    void getRiskScore() {
        // Compute first
        auth(adminToken).when().post("/risk/compute/3")
                .then().statusCode(200);

        auth(adminToken)
                .when().get("/risk/score/3")
                .then().statusCode(200)
                .body("score", notNullValue());
    }

    @Test
    void getHighRiskUsers() {
        auth(adminToken)
                .when().get("/risk/high-risk")
                .then().statusCode(200)
                .body("$", instanceOf(List.class));
    }

    @Test
    void getHighRiskUsersWithThreshold() {
        auth(adminToken)
                .when().get("/risk/high-risk?threshold=0")
                .then().statusCode(200)
                .body("$", instanceOf(List.class));
    }

    @Test
    void getRiskEventsByUser() {
        recordRiskEvent(adminToken, 3, "MISIDENTIFICATION");

        auth(adminToken)
                .when().get("/risk/events/3")
                .then().statusCode(200)
                .body("$", instanceOf(List.class));
    }

    @Test
    void getAuditTrailForProduct() {
        // Create a product to generate audit entry
        Map<String, Object> product = new HashMap<>();
        product.put("name", "Audit Test Product " + uid());
        product.put("description", "For audit test");
        product.put("price", 9.99);
        product.put("stockQuantity", 10);
        product.put("categoryId", 1);

        int pid = auth(login("seller"))
                .body(product)
                .when().post("/products")
                .then().statusCode(200)
                .extract().path("id");

        auth(adminToken)
                .when().get("/audit/PRODUCT/" + pid)
                .then().statusCode(200)
                .body("$", instanceOf(List.class));
    }

    @Test
    void getAuditTrailForOrder() {
        // Place an order to generate audit entry
        Map<String, Object> orderBody = new HashMap<>();
        orderBody.put("productId", 1);
        orderBody.put("quantity", 1);
        orderBody.put("inventoryItemId", 1);

        int orderId = auth(memberToken)
                .body(orderBody)
                .when().post("/orders")
                .then().statusCode(200)
                .extract().path("id");

        auth(adminToken)
                .when().get("/audit/ORDER/" + orderId)
                .then().statusCode(200)
                .body("$", instanceOf(List.class));
    }

    @Test
    void auditEntriesHaveActorAndTimestamp() {
        // Use product audit from earlier
        Map<String, Object> product = new HashMap<>();
        product.put("name", "Audit Fields Test " + uid());
        product.put("description", "For audit fields check");
        product.put("price", 5.00);
        product.put("stockQuantity", 5);
        product.put("categoryId", 1);

        int pid = auth(login("seller"))
                .body(product)
                .when().post("/products")
                .then().statusCode(200)
                .extract().path("id");

        Response auditResp = auth(adminToken)
                .when().get("/audit/PRODUCT/" + pid)
                .then().statusCode(200)
                .body("$", not(empty()))
                .extract().response();

        // Each entry must have actorId and createdAt
        List<Object> actorIds = auditResp.jsonPath().getList("actorId");
        List<String> createdAts = auditResp.jsonPath().getList("createdAt");
        assert actorIds != null && !actorIds.isEmpty();
        assert createdAts != null && !createdAts.isEmpty();
    }

    @Test
    void memberCannotGetHighRiskUsers() {
        auth(memberToken)
                .when().get("/risk/high-risk")
                .then().statusCode(403);
    }

    @Test
    void memberCannotComputeRiskScore() {
        auth(memberToken)
                .when().post("/risk/compute/3")
                .then().statusCode(403);
    }
}

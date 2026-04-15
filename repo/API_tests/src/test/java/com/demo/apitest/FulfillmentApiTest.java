package com.demo.apitest;

import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Tests: POST /fulfillments, POST /fulfillments/{id}/advance,
 *        POST /fulfillments/{id}/cancel, GET /fulfillments/order/{orderId},
 *        GET /fulfillments/{id}/steps
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FulfillmentApiTest extends BaseApiTest {

    private String warehouseToken;
    private String memberToken;

    @BeforeAll
    void setup() {
        warehouseToken = login("warehouse");
        // Register a unique member per test-class run to isolate the rate-limit bucket.
        String unique = uid();
        java.util.Map<String, String> reg = new java.util.HashMap<>();
        reg.put("username", "fulfil_" + unique);
        reg.put("email", "fulfil_" + unique + "@example.com");
        reg.put("password", "password123");
        reg.put("displayName", "Fulfillment Test User");
        memberToken = given().contentType(io.restassured.http.ContentType.JSON).body(reg)
                .when().post("/auth/register")
                .then().statusCode(200)
                .extract().path("token");
    }

    private int createOrder() {
        Map<String, Object> body = new HashMap<>();
        body.put("productId", 1);
        body.put("quantity", 1);
        body.put("inventoryItemId", 1);

        return auth(memberToken)
                .body(body)
                .when().post("/orders")
                .then().statusCode(200)
                .extract().path("id");
    }

    private int createFulfillment(int orderId, String key) {
        Map<String, Object> body = new HashMap<>();
        body.put("orderId", orderId);
        body.put("warehouseId", 1);
        body.put("idempotencyKey", key);

        return auth(warehouseToken)
                .body(body)
                .when().post("/fulfillments")
                .then().statusCode(200)
                .extract().path("id");
    }

    @Test
    void createFulfillmentAndAdvanceSteps() {
        int orderId = createOrder();
        int fid = createFulfillment(orderId, uid());

        // Advance through PICK -> PACK -> SHIP -> DELIVER
        for (String step : new String[]{"PICK", "PACK", "SHIP", "DELIVER"}) {
            Map<String, Object> body = new HashMap<>();
            body.put("stepName", step);
            body.put("notes", "Integration test: " + step);

            auth(warehouseToken)
                    .body(body)
                    .when().post("/fulfillments/" + fid + "/advance")
                    .then().statusCode(200);
        }
    }

    @Test
    void getFulfillmentSteps() {
        int orderId = createOrder();
        int fid = createFulfillment(orderId, uid());

        auth(warehouseToken)
                .when().get("/fulfillments/" + fid + "/steps")
                .then().statusCode(200)
                .body("$", instanceOf(List.class));
    }

    @Test
    void getFulfillmentByOrderId() {
        int orderId = createOrder();
        createFulfillment(orderId, uid());

        auth(warehouseToken)
                .when().get("/fulfillments/order/" + orderId)
                .then().statusCode(200)
                .body("orderId", equalTo(orderId));
    }

    @Test
    void fulfillmentIdempotency() {
        int orderId = createOrder();
        String key = uid();

        int id1 = createFulfillment(orderId, key);

        Map<String, Object> body = new HashMap<>();
        body.put("orderId", orderId);
        body.put("warehouseId", 1);
        body.put("idempotencyKey", key);

        int id2 = auth(warehouseToken)
                .body(body)
                .when().post("/fulfillments")
                .then().statusCode(200)
                .extract().path("id");

        assert id1 == id2 : "Same idempotency key should return same fulfillment";
    }

    @Test
    void cancelFulfillment() {
        int orderId = createOrder();
        int fid = createFulfillment(orderId, uid());

        // Cancel before advancing — should be in CREATED state
        auth(warehouseToken)
                .when().post("/fulfillments/" + fid + "/cancel")
                .then().statusCode(anyOf(is(200), is(409)));
    }

    @Test
    void memberCannotCreateFulfillment() {
        int orderId = createOrder();

        Map<String, Object> body = new HashMap<>();
        body.put("orderId", orderId);
        body.put("warehouseId", 1);
        body.put("idempotencyKey", uid());

        auth(memberToken)
                .body(body)
                .when().post("/fulfillments")
                .then().statusCode(403);
    }
}

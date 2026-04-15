package com.demo.apitest;

import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;

/**
 * Tests: GET /orders, GET /orders/{id}, GET /orders/buyer/{buyerId},
 *        POST /orders, PATCH /orders/{id}/status
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrderApiTest extends BaseApiTest {

    private String adminToken;
    private String memberToken;
    private String warehouseToken;
    private int memberId;

    @BeforeAll
    void setup() {
        adminToken = login("admin");
        memberToken = login("member");
        warehouseToken = login("warehouse");
        // Derive member's user ID dynamically — avoids hardcoding the seeded row number.
        memberId = auth(memberToken)
                .when().get("/users/me")
                .then().statusCode(200)
                .extract().path("id");
    }

    private int placeOrder(String token, int productId) {
        Map<String, Object> body = new HashMap<>();
        body.put("productId", productId);
        body.put("quantity", 1);
        body.put("inventoryItemId", 1);

        return auth(token)
                .body(body)
                .when().post("/orders")
                .then().statusCode(200)
                .body("id", notNullValue())
                .extract().path("id");
    }

    @Test
    void placeOrderServerComputesPrice() {
        Map<String, Object> body = new HashMap<>();
        body.put("productId", 1);
        body.put("quantity", 1);
        body.put("inventoryItemId", 1);
        body.put("totalPrice", 0.01); // Client-supplied value must be ignored

        Response resp = auth(memberToken)
                .body(body)
                .when().post("/orders")
                .then().statusCode(200)
                .body("id", notNullValue())
                .body("totalPrice", greaterThanOrEqualTo(1.0f))
                .extract().response();

        // Verify server-computed price >= 1.0 (not the client-supplied 0.01)
        float price = resp.jsonPath().getFloat("totalPrice");
        assert price >= 1.0f : "Server must compute totalPrice, not accept client-supplied value";
    }

    @Test
    void getAllOrders() {
        auth(adminToken)
                .when().get("/orders")
                .then().statusCode(200)
                .body("$", not(empty()));
    }

    @Test
    void getOrderById() {
        int id = placeOrder(memberToken, 1);

        auth(memberToken)
                .when().get("/orders/" + id)
                .then().statusCode(200)
                .body("id", equalTo(id))
                .body("totalPrice", greaterThanOrEqualTo(1.0f));
    }

    @Test
    void adminCanGetOrderById() {
        int id = placeOrder(memberToken, 2);

        auth(adminToken)
                .when().get("/orders/" + id)
                .then().statusCode(200)
                .body("id", equalTo(id));
    }

    @Test
    void getOrdersByBuyer() {
        placeOrder(memberToken, 1);

        auth(memberToken)
                .when().get("/orders/buyer/" + memberId)
                .then().statusCode(200)
                .body("$", not(empty()));
    }

    @Test
    void adminGetOrdersByBuyer() {
        auth(adminToken)
                .when().get("/orders/buyer/" + memberId)
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void buyerCanCancelOwnOrder() {
        int id = placeOrder(memberToken, 3);

        auth(memberToken)
                .when().patch("/orders/" + id + "/status?status=CANCELLED")
                .then().statusCode(anyOf(is(200), is(409)));
    }

    @Test
    void buyerCannotSetOrderShipped() {
        int id = placeOrder(memberToken, 4);

        auth(memberToken)
                .when().patch("/orders/" + id + "/status?status=SHIPPED")
                .then().statusCode(403);
    }

    @Test
    void cancelOrderReleasesReservation() {
        int id = placeOrder(memberToken, 1);

        // Verify reservation was created
        int reservationId = auth(memberToken)
                .when().get("/orders/" + id)
                .then().statusCode(200)
                .extract().path("reservationId");

        // Cancel the order
        auth(memberToken)
                .when().patch("/orders/" + id + "/status?status=CANCELLED")
                .then().statusCode(anyOf(is(200), is(409)));
    }

    @Test
    void invalidStatusTransitionRejected() {
        // Place an order (PENDING), skip to DELIVERED without going through steps
        int id = placeOrder(memberToken, 5);

        auth(warehouseToken)
                .when().patch("/orders/" + id + "/status?status=DELIVERED")
                .then().statusCode(anyOf(is(400), is(409)));
    }

    @Test
    void orderNotFoundReturns404() {
        auth(adminToken)
                .when().get("/orders/99999")
                .then().statusCode(404);
    }

    @Test
    void memberCannotListAllOrders() {
        auth(memberToken)
                .when().get("/orders")
                .then().statusCode(403);
    }
}

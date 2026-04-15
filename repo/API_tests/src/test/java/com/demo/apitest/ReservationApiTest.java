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
 * Tests: POST /reservations, POST /reservations/{id}/confirm,
 *        POST /reservations/{id}/cancel, GET /reservations/my
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReservationApiTest extends BaseApiTest {

    private String warehouseToken;
    private String memberToken;

    @BeforeAll
    void setup() {
        warehouseToken = login("warehouse");
        memberToken = login("member");
    }

    private Map<String, Object> reservationBody(int itemId, String key) {
        Map<String, Object> body = new HashMap<>();
        body.put("inventoryItemId", itemId);
        body.put("quantity", 1);
        body.put("idempotencyKey", key);
        return body;
    }

    @Test
    void createReservationAndConfirm() {
        String key = uid();
        Response resp = auth(warehouseToken)
                .body(reservationBody(1, key))
                .when().post("/reservations")
                .then().statusCode(200)
                .body("status", equalTo("HELD"))
                .body("id", notNullValue())
                .extract().response();

        int id = resp.path("id");

        auth(warehouseToken)
                .when().post("/reservations/" + id + "/confirm")
                .then().statusCode(200)
                .body("status", equalTo("CONFIRMED"));
    }

    @Test
    void createReservationAndCancel() {
        // Snapshot reserved qty before
        Response invBefore = auth(warehouseToken)
                .when().get("/inventory/product/2")
                .then().statusCode(200)
                .extract().response();

        List<Integer> itemIds = invBefore.jsonPath().getList("id");
        int itemId = itemIds.get(0);
        int reservedBefore = invBefore.jsonPath().getInt("[0].quantityReserved");

        String key = uid();
        int id = auth(warehouseToken)
                .body(reservationBody(itemId, key))
                .when().post("/reservations")
                .then().statusCode(200)
                .body("status", equalTo("HELD"))
                .extract().path("id");

        auth(warehouseToken)
                .when().post("/reservations/" + id + "/cancel")
                .then().statusCode(200)
                .body("status", equalTo("CANCELLED"));

        // Verify reserved qty rolled back
        int reservedAfter = auth(warehouseToken)
                .when().get("/inventory/product/2")
                .then().statusCode(200)
                .extract().jsonPath().getInt("[0].quantityReserved");

        assert reservedAfter == reservedBefore : "Reserved qty should roll back after cancel";
    }

    @Test
    void reservationIdempotency() {
        String key = uid();
        Map<String, Object> payload = reservationBody(1, key);

        int id1 = auth(warehouseToken).body(payload).when().post("/reservations")
                .then().statusCode(200).extract().path("id");
        int id2 = auth(warehouseToken).body(payload).when().post("/reservations")
                .then().statusCode(200).extract().path("id");

        assert id1 == id2 : "Same idempotency key should return same reservation id";
    }

    @Test
    void getMyReservations() {
        auth(memberToken)
                .when().get("/reservations/my")
                .then().statusCode(200)
                .body("$", instanceOf(List.class));
    }

    @Test
    void zeroQuantityRejected() {
        Map<String, Object> body = new HashMap<>();
        body.put("inventoryItemId", 1);
        body.put("quantity", 0);
        body.put("idempotencyKey", uid());

        auth(warehouseToken)
                .body(body)
                .when().post("/reservations")
                .then().statusCode(400);
    }

    @Test
    void negativeQuantityRejected() {
        Map<String, Object> body = new HashMap<>();
        body.put("inventoryItemId", 1);
        body.put("quantity", -5);
        body.put("idempotencyKey", uid());

        auth(warehouseToken)
                .body(body)
                .when().post("/reservations")
                .then().statusCode(400);
    }

    @Test
    void userCannotConfirmOthersReservation() {
        // Warehouse creates reservation, member tries to confirm
        String key = uid();
        int id = auth(warehouseToken)
                .body(reservationBody(1, key))
                .when().post("/reservations")
                .then().statusCode(200)
                .extract().path("id");

        auth(memberToken)
                .when().post("/reservations/" + id + "/confirm")
                .then().statusCode(403);
    }

    @Test
    void confirmNonExistentReservationNotFound() {
        auth(warehouseToken)
                .when().post("/reservations/99999/confirm")
                .then().statusCode(404);
    }
}

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
 * Tests: GET /inventory/product/{productId}, GET /inventory/low-stock,
 *        POST /inventory/adjust, POST /inventory/inbound,
 *        POST /inventory/outbound, POST /inventory/stocktake
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InventoryApiTest extends BaseApiTest {

    private String warehouseToken;
    private String memberToken;

    @BeforeAll
    void setup() {
        warehouseToken = login("warehouse");
        memberToken = login("member");
    }

    @Test
    void getInventoryByProduct() {
        auth(warehouseToken)
                .when().get("/inventory/product/1")
                .then().statusCode(200)
                .body("$", not(empty()));
    }

    @Test
    void getLowStockItems() {
        auth(warehouseToken)
                .when().get("/inventory/low-stock")
                .then().statusCode(200)
                .body("$", instanceOf(List.class));
    }

    @Test
    void inventoryItemHasWarehouseName() {
        Response resp = auth(warehouseToken)
                .when().get("/inventory/product/1")
                .then().statusCode(200)
                .extract().response();

        List<String> names = resp.jsonPath().getList("warehouseName");
        assert names != null && !names.isEmpty() : "warehouseName must be present";
        assert names.get(0) != null : "warehouseName must not be null";
    }

    @Test
    void adjustStock() {
        Map<String, Object> body = new HashMap<>();
        body.put("inventoryItemId", 1);
        body.put("quantityChange", 5);
        body.put("movementType", "ADJUSTMENT");
        body.put("referenceDocument", "TEST-ADJ-" + uid());
        body.put("notes", "Integration test adjustment");

        auth(warehouseToken)
                .body(body)
                .when().post("/inventory/adjust")
                .then().statusCode(200);
    }

    @Test
    void recordInboundMovement() {
        Map<String, Object> body = new HashMap<>();
        body.put("inventoryItemId", 1);
        body.put("quantity", 10);
        body.put("referenceDocument", "PO-" + uid());
        body.put("notes", "Inbound test");

        auth(warehouseToken)
                .body(body)
                .when().post("/inventory/inbound")
                .then().statusCode(200);
    }

    @Test
    void recordOutboundMovement() {
        // First add stock to ensure enough for outbound
        Map<String, Object> inbound = new HashMap<>();
        inbound.put("inventoryItemId", 1);
        inbound.put("quantity", 20);
        inbound.put("referenceDocument", "PO-IN-" + uid());

        auth(warehouseToken).body(inbound).when().post("/inventory/inbound")
                .then().statusCode(200);

        Map<String, Object> body = new HashMap<>();
        body.put("inventoryItemId", 1);
        body.put("quantity", 2);
        body.put("referenceDocument", "SO-" + uid());
        body.put("notes", "Outbound test");

        auth(warehouseToken)
                .body(body)
                .when().post("/inventory/outbound")
                .then().statusCode(200);
    }

    @Test
    void recordStocktake() {
        Map<String, Object> body = new HashMap<>();
        body.put("productId", 1);
        body.put("warehouseId", 1);
        body.put("countedQuantity", 50);
        body.put("referenceDocument", "ST-" + uid());

        auth(warehouseToken)
                .body(body)
                .when().post("/inventory/stocktake")
                .then().statusCode(200);
    }

    @Test
    void memberCannotAccessLowStock() {
        auth(memberToken)
                .when().get("/inventory/low-stock")
                .then().statusCode(403);
    }

    @Test
    void memberCannotAdjustStock() {
        Map<String, Object> body = new HashMap<>();
        body.put("inventoryItemId", 1);
        body.put("quantityChange", 5);
        body.put("movementType", "ADJUSTMENT");
        body.put("referenceDocument", "UNAUTH-ADJ-" + uid());

        auth(memberToken)
                .body(body)
                .when().post("/inventory/adjust")
                .then().statusCode(403);
    }
}

package com.demo.apitest;

import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;

/**
 * Tests: GET /warehouses, GET /warehouses/{id}, POST /warehouses, PUT /warehouses/{id}
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WarehouseApiTest extends BaseApiTest {

    private String adminToken;
    private String sellerToken;

    @BeforeAll
    void setup() {
        adminToken = login("admin");
        sellerToken = login("seller");
    }

    @Test
    void getWarehouses() {
        auth(adminToken)
                .when().get("/warehouses")
                .then().statusCode(200)
                .body("$", not(empty()))
                .body("size()", greaterThanOrEqualTo(3))
                .body("[0].id", notNullValue())
                .body("[0].name", not(emptyOrNullString()));
    }

    @Test
    void getWarehouseById() {
        auth(adminToken)
                .when().get("/warehouses/1")
                .then().statusCode(200)
                .body("id", equalTo(1))
                .body("name", not(emptyOrNullString()));
    }

    @Test
    void getWarehouseByIdNotFound() {
        auth(adminToken)
                .when().get("/warehouses/99999")
                .then().statusCode(400);
    }

    @Test
    void createWarehouse() {
        String unique = uid();
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Test Warehouse " + unique);
        body.put("code", "TW" + unique.substring(0, 4).toUpperCase());
        body.put("location", "123 Test St");
        body.put("active", true);

        int id = auth(adminToken)
                .body(body)
                .when().post("/warehouses")
                .then().statusCode(200)
                .body("id", notNullValue())
                .body("name", containsString("Test Warehouse"))
                .extract().path("id");

        assert id > 0;
    }

    @Test
    void updateWarehouse() {
        // Create first, then update
        String unique = uid();
        Map<String, Object> create = new HashMap<>();
        create.put("name", "WH Before " + unique);
        create.put("code", "WB" + unique.substring(0, 4).toUpperCase());
        create.put("location", "1 Before St");
        create.put("active", true);

        int id = auth(adminToken)
                .body(create)
                .when().post("/warehouses")
                .then().statusCode(200)
                .extract().path("id");

        Map<String, Object> update = new HashMap<>();
        update.put("name", "WH After " + unique);
        update.put("code", "WA" + unique.substring(0, 4).toUpperCase());
        update.put("location", "1 After St");
        update.put("active", true);

        auth(adminToken)
                .body(update)
                .when().put("/warehouses/" + id)
                .then().statusCode(200);
    }

    @Test
    void nonAdminCannotCreateWarehouse() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Unauthorized WH");
        body.put("code", "UWH1");
        body.put("location", "Nowhere");
        body.put("active", true);

        auth(sellerToken)
                .body(body)
                .when().post("/warehouses")
                .then().statusCode(403);
    }

    @Test
    void unauthenticatedCannotCreateWarehouse() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Unauth WH");
        body.put("code", "UAWH");
        body.put("location", "Nowhere");
        body.put("active", true);

        noAuth()
                .body(body)
                .when().post("/warehouses")
                .then().statusCode(403);
    }
}

package com.demo.apitest;

import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;

/**
 * Tests: GET /products, GET /products/{id}, GET /products/seller/{sellerId},
 *        GET /products/status/{status}, POST /products,
 *        PUT /products/{id}, DELETE /products/{id}
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProductApiTest extends BaseApiTest {

    private String adminToken;
    private String sellerToken;
    private String memberToken;

    @BeforeAll
    void setup() {
        adminToken = login("admin");
        sellerToken = login("seller");
        memberToken = login("member");
    }

    @Test
    void getAllProducts() {
        noAuth()
                .when().get("/products")
                .then().statusCode(200)
                .body("$", not(empty()));
    }

    @Test
    void getProductById() {
        noAuth()
                .when().get("/products/1")
                .then().statusCode(200)
                .body("id", equalTo(1))
                .body("name", not(emptyOrNullString()))
                .body("sellerId", notNullValue());
    }

    @Test
    void getProductByIdNotFound() {
        noAuth()
                .when().get("/products/99999")
                .then().statusCode(400);
    }

    @Test
    void getProductsBySellerPublic() {
        // Seller user has id=3 in seed data
        noAuth()
                .when().get("/products/seller/3")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void getProductsByStatus() {
        noAuth()
                .when().get("/products/status/APPROVED")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void createProduct() {
        String unique = uid();
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Test Product " + unique);
        body.put("description", "Created by integration test");
        body.put("price", 29.99);
        body.put("stockQuantity", 50);
        body.put("categoryId", 1);

        auth(sellerToken)
                .body(body)
                .when().post("/products")
                .then().statusCode(200)
                .body("id", notNullValue())
                .body("name", containsString("Test Product"))
                .body("sellerId", notNullValue());
    }

    @Test
    void createProductSellerIdBoundToAuthUser() {
        // Seller tries to supply a spoofed sellerId — server must override to the authenticated user's id
        String unique = uid();
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Seller Bind Test " + unique);
        body.put("description", "sellerId override test");
        body.put("price", 5.0);
        body.put("stockQuantity", 10);
        body.put("categoryId", 1);
        body.put("sellerId", 99999); // Must be overridden

        auth(sellerToken)
                .body(body)
                .when().post("/products")
                .then().statusCode(200)
                .body("id", notNullValue())
                .body("name", containsString("Seller Bind Test"))
                .body("sellerId", not(equalTo(99999)));
    }

    @Test
    void updateProduct() {
        // Create a product, then update it
        String unique = uid();
        Map<String, Object> create = new HashMap<>();
        create.put("name", "Prod Before " + unique);
        create.put("description", "Before update");
        create.put("price", 10.00);
        create.put("stockQuantity", 10);
        create.put("categoryId", 1);

        int id = auth(sellerToken)
                .body(create)
                .when().post("/products")
                .then().statusCode(200)
                .extract().path("id");

        // Fetch current status so the full ProductDto can be sent (PUT requires all fields including status)
        String currentStatus = auth(sellerToken)
                .when().get("/products/" + id)
                .then().statusCode(200)
                .extract().path("status");

        Map<String, Object> update = new HashMap<>();
        update.put("name", "Prod After " + unique);
        update.put("description", "After update");
        update.put("price", 15.00);
        update.put("stockQuantity", 20);
        update.put("categoryId", 1);
        update.put("status", currentStatus);

        auth(sellerToken)
                .body(update)
                .when().put("/products/" + id)
                .then().statusCode(200);
    }

    @Test
    void deleteProduct() {
        // Create a product, then delete it
        String unique = uid();
        Map<String, Object> create = new HashMap<>();
        create.put("name", "Prod Del " + unique);
        create.put("description", "Will be deleted");
        create.put("price", 5.00);
        create.put("stockQuantity", 5);
        create.put("categoryId", 1);

        int id = auth(sellerToken)
                .body(create)
                .when().post("/products")
                .then().statusCode(200)
                .extract().path("id");

        auth(sellerToken)
                .when().delete("/products/" + id)
                .then().statusCode(204);
    }

    @Test
    void sellerCannotModifyOtherSellersProduct() {
        // Admin creates a product; seeded seller attempts to update it
        String unique = uid();
        Map<String, Object> adminProduct = new HashMap<>();
        adminProduct.put("name", "Admin Product " + unique);
        adminProduct.put("description", "Owned by admin");
        adminProduct.put("price", 99.00);
        adminProduct.put("stockQuantity", 1);
        adminProduct.put("categoryId", 1);

        int adminPid = auth(adminToken)
                .body(adminProduct)
                .when().post("/products")
                .then().statusCode(200)
                .extract().path("id");

        Map<String, Object> update = new HashMap<>();
        update.put("name", "Hijacked Product");
        update.put("price", 1.00);
        update.put("stockQuantity", 1);
        update.put("categoryId", 1);

        auth(sellerToken)
                .body(update)
                .when().put("/products/" + adminPid)
                .then().statusCode(403);
    }

    @Test
    void memberCannotCreateProduct() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Member Product Attempt");
        body.put("description", "Should fail");
        body.put("price", 1.00);
        body.put("stockQuantity", 1);
        body.put("categoryId", 1);

        auth(memberToken)
                .body(body)
                .when().post("/products")
                .then().statusCode(403);
    }
}

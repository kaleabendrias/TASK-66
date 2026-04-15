package com.demo.apitest;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;

/**
 * Tests: GET /categories, GET /categories/{id},
 *        POST /categories, PUT /categories/{id}, DELETE /categories/{id}
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CategoryApiTest extends BaseApiTest {

    private String adminToken;
    private String memberToken;

    @BeforeAll
    void setup() {
        adminToken = login("admin");
        memberToken = login("member");
    }

    @Test
    void getAllCategories() {
        noAuth()
                .when().get("/categories")
                .then().statusCode(200)
                .body("$", not(empty()));
    }

    @Test
    void getCategoryById() {
        noAuth()
                .when().get("/categories/1")
                .then().statusCode(200)
                .body("id", equalTo(1))
                .body("name", not(emptyOrNullString()));
    }

    @Test
    void getCategoryByIdNotFound() {
        noAuth()
                .when().get("/categories/99999")
                .then().statusCode(400);
    }

    @Test
    void createCategory() {
        String unique = uid();
        Map<String, String> body = new HashMap<>();
        body.put("name", "Test Category " + unique);
        body.put("description", "A test category created by integration tests");

        auth(adminToken)
                .body(body)
                .when().post("/categories")
                .then().statusCode(200)
                .body("id", notNullValue())
                .body("name", containsString("Test Category"));
    }

    @Test
    void updateCategory() {
        // Create then update
        String unique = uid();
        Map<String, String> create = new HashMap<>();
        create.put("name", "Cat Before " + unique);
        create.put("description", "Before update");

        int id = auth(adminToken)
                .body(create)
                .when().post("/categories")
                .then().statusCode(200)
                .body("id", notNullValue())
                .extract().path("id");

        Map<String, String> update = new HashMap<>();
        update.put("name", "Cat After " + unique);
        update.put("description", "After update");

        auth(adminToken)
                .body(update)
                .when().put("/categories/" + id)
                .then().statusCode(200);
    }

    @Test
    void deleteCategory() {
        // Create then delete
        String unique = uid();
        Map<String, String> create = new HashMap<>();
        create.put("name", "Cat Del " + unique);
        create.put("description", "Will be deleted");

        int id = auth(adminToken)
                .body(create)
                .when().post("/categories")
                .then().statusCode(200)
                .extract().path("id");

        auth(adminToken)
                .when().delete("/categories/" + id)
                .then().statusCode(204);
    }

    @Test
    void deleteCategoryNotFound() {
        auth(adminToken)
                .when().delete("/categories/99999")
                .then().statusCode(400);
    }

    @Test
    void memberCannotCreateCategory() {
        Map<String, String> body = new HashMap<>();
        body.put("name", "Unauthorized Category");
        body.put("description", "Should fail");

        auth(memberToken)
                .body(body)
                .when().post("/categories")
                .then().statusCode(403);
    }

    @Test
    void unauthenticatedCannotCreateCategory() {
        Map<String, String> body = new HashMap<>();
        body.put("name", "Unauth Category");
        body.put("description", "Should fail");

        noAuth()
                .body(body)
                .when().post("/categories")
                .then().statusCode(403);
    }
}

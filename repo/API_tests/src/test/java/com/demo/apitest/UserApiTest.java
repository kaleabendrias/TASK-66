package com.demo.apitest;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Tests: GET /users, GET /users/{id}, GET /users/me, PUT /users/{id}, DELETE /users/{id}
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserApiTest extends BaseApiTest {

    private String adminToken;
    private String memberToken;

    @BeforeAll
    void setup() {
        adminToken = login("admin");
        memberToken = login("member");
    }

    @Test
    void getAllUsers() {
        auth(adminToken)
                .when().get("/users")
                .then().statusCode(200)
                .body("$", not(empty()));
    }

    @Test
    void getUserById() {
        // Admin user has id=6 based on seed data ordering (guest=1,member=2,seller=3,warehouse=4,moderator=5,admin=6)
        auth(adminToken)
                .when().get("/users/1")
                .then().statusCode(200)
                .body("id", equalTo(1));
    }

    @Test
    void getUserByIdNotFound() {
        auth(adminToken)
                .when().get("/users/99999")
                .then().statusCode(400);
    }

    @Test
    void getMeReturnsCurrentUser() {
        auth(memberToken)
                .when().get("/users/me")
                .then().statusCode(200)
                .body("username", equalTo("member"))
                .body("id", notNullValue());
    }

    @Test
    void updateUser() {
        // Register a user to update, then update their displayName
        String unique = uid();
        Map<String, String> reg = new HashMap<>();
        reg.put("username", "upd_" + unique);
        reg.put("email", "upd_" + unique + "@example.com");
        reg.put("password", "password123");
        reg.put("displayName", "Before Update");

        Response regResp = given().contentType(ContentType.JSON).body(reg)
                .when().post("/auth/register")
                .then().statusCode(200)
                .extract().response();

        String newToken = regResp.path("token");

        // Get the new user's id via /users/me
        int userId = given()
                .header("Authorization", "Bearer " + newToken)
                .when().get("/users/me")
                .then().statusCode(200)
                .extract().path("id");

        // Fetch the current user record so we can send a complete UserDto (PUT requires all fields)
        io.restassured.response.Response current = given()
                .header("Authorization", "Bearer " + adminToken)
                .when().get("/users/" + userId)
                .then().statusCode(200)
                .extract().response();

        Map<String, Object> update = new HashMap<>();
        update.put("username", (String) current.path("username"));
        update.put("email", (String) current.path("email"));
        update.put("displayName", "After Update");
        update.put("role", (String) current.path("role"));
        update.put("enabled", (Boolean) current.path("enabled"));

        // Admin updates the user's record
        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(update)
                .when().put("/users/" + userId)
                .then().statusCode(200);
    }

    @Test
    void deleteUser() {
        // Register a fresh user, then admin deletes them
        String unique = uid();
        Map<String, String> reg = new HashMap<>();
        reg.put("username", "del_" + unique);
        reg.put("email", "del_" + unique + "@example.com");
        reg.put("password", "password123");
        reg.put("displayName", "To Delete");

        Response regResp = given().contentType(ContentType.JSON).body(reg)
                .when().post("/auth/register")
                .then().statusCode(200)
                .extract().response();

        String newToken = regResp.path("token");
        int userId = given()
                .header("Authorization", "Bearer " + newToken)
                .when().get("/users/me")
                .then().statusCode(200)
                .extract().path("id");

        auth(adminToken)
                .when().delete("/users/" + userId)
                .then().statusCode(204);
    }

    @Test
    void memberCannotListAllUsers() {
        auth(memberToken)
                .when().get("/users")
                .then().statusCode(403);
    }

    @Test
    void unauthenticatedCannotListUsers() {
        noAuth()
                .when().get("/users")
                .then().statusCode(403);
    }
}

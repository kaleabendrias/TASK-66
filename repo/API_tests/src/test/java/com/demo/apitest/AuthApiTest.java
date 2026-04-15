package com.demo.apitest;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Tests: POST /auth/login, POST /auth/register, GET /users/me
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthApiTest extends BaseApiTest {

    private String adminToken;

    @BeforeAll
    void setup() {
        adminToken = login("admin");
    }

    @Test
    void loginSuccess() {
        for (String user : new String[]{"seller", "moderator"}) {
            Map<String, String> body = new HashMap<>();
            body.put("username", user);
            body.put("password", "password123");

            given().contentType(ContentType.JSON).body(body)
                    .when().post("/auth/login")
                    .then().statusCode(200)
                    .body("token", not(emptyOrNullString()))
                    .body("username", equalTo(user));
        }
    }

    @Test
    void loginWrongPassword() {
        Map<String, String> body = new HashMap<>();
        body.put("username", "seller");
        body.put("password", "wrongpassword");

        given().contentType(ContentType.JSON).body(body)
                .when().post("/auth/login")
                .then().statusCode(anyOf(is(400), is(401), is(403)));
    }

    @Test
    void loginNonExistentUser() {
        Map<String, String> body = new HashMap<>();
        body.put("username", "no_such_user_xyz_" + uid());
        body.put("password", "password123");

        given().contentType(ContentType.JSON).body(body)
                .when().post("/auth/login")
                .then().statusCode(anyOf(is(401), is(403), is(404)));
    }

    @Test
    void registerNewUser() {
        String unique = uid();
        Map<String, String> body = new HashMap<>();
        body.put("username", "testuser_" + unique);
        body.put("email", "test_" + unique + "@example.com");
        body.put("password", "password123");
        body.put("displayName", "Test User " + unique);

        given().contentType(ContentType.JSON).body(body)
                .when().post("/auth/register")
                .then().statusCode(200)
                .body("token", not(emptyOrNullString()))
                .body("username", equalTo("testuser_" + unique));
    }

    @Test
    void registerReturnsRoleField() {
        String unique = uid();
        Map<String, String> body = new HashMap<>();
        body.put("username", "roletest_" + unique);
        body.put("email", "roletest_" + unique + "@example.com");
        body.put("password", "password123");
        body.put("displayName", "Role Test User");

        given().contentType(ContentType.JSON).body(body)
                .when().post("/auth/register")
                .then().statusCode(200)
                .body("token", not(emptyOrNullString()))
                .body("role", not(emptyOrNullString()));
    }

    @Test
    void registerDuplicateUsername() {
        Map<String, String> body = new HashMap<>();
        body.put("username", "admin");
        body.put("email", "dup_" + uid() + "@example.com");
        body.put("password", "password123");
        body.put("displayName", "Duplicate");

        given().contentType(ContentType.JSON).body(body)
                .when().post("/auth/register")
                .then().statusCode(anyOf(is(400), is(409)));
    }

    @Test
    void tokenHasThreeParts() {
        String token = login("seller");
        String[] parts = token.split("\\.");
        assert parts.length == 3 : "JWT should have 3 dot-separated parts";
    }

    @Test
    void getMeEndpoint() {
        auth(adminToken)
                .when().get("/users/me")
                .then().statusCode(200)
                .body("username", equalTo("admin"))
                .body("id", notNullValue());
    }

    @Test
    void getMeReturnsTokenFields() {
        String token = login("member");
        auth(token)
                .when().get("/users/me")
                .then().statusCode(200)
                .body("username", equalTo("member"))
                .body("id", notNullValue())
                .body("role", not(emptyOrNullString()));
    }
}

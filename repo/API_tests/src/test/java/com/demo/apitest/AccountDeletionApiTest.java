package com.demo.apitest;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Tests: POST /account-deletion/request, POST /account-deletion/{id}/cancel,
 *        GET /account-deletion/status
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountDeletionApiTest extends BaseApiTest {

    private String freshUserToken() {
        // Register a unique user for each deletion test
        String unique = uid();
        Map<String, String> reg = new HashMap<>();
        reg.put("username", "del_user_" + unique);
        reg.put("email", "del_" + unique + "@example.com");
        reg.put("password", "password123");
        reg.put("displayName", "Deletion User");

        return given()
                .contentType(ContentType.JSON)
                .body(reg)
                .when().post("/auth/register")
                .then().statusCode(200)
                .extract().path("token");
    }

    @Test
    void requestAccountDeletion() {
        String token = freshUserToken();

        auth(token)
                .when().post("/account-deletion/request")
                .then().statusCode(200)
                .body("status", not(emptyOrNullString()))
                .body("coolingOffEndsAt", notNullValue());
    }

    @Test
    void getDeletionStatus() {
        String token = freshUserToken();

        auth(token)
                .when().post("/account-deletion/request")
                .then().statusCode(200);

        auth(token)
                .when().get("/account-deletion/status")
                .then().statusCode(200)
                .body("status", not(emptyOrNullString()));
    }

    @Test
    void cancelDeletionRequest() {
        String token = freshUserToken();

        int id = auth(token)
                .when().post("/account-deletion/request")
                .then().statusCode(200)
                .extract().path("id");

        auth(token)
                .when().post("/account-deletion/" + id + "/cancel")
                .then().statusCode(200);
    }

    @Test
    void duplicateDeletionRequestRejected() {
        String token = freshUserToken();

        auth(token).when().post("/account-deletion/request")
                .then().statusCode(200);

        // Second request for same account should conflict
        auth(token).when().post("/account-deletion/request")
                .then().statusCode(anyOf(is(400), is(409)));
    }

    @Test
    void coolingOffPeriodIsPresent() {
        String token = freshUserToken();

        auth(token)
                .when().post("/account-deletion/request")
                .then().statusCode(200)
                .body("coolingOffEndsAt", notNullValue());
    }

    @Test
    void unauthenticatedCannotRequestDeletion() {
        noAuth()
                .when().post("/account-deletion/request")
                .then().statusCode(403);
    }

    @Test
    void unauthenticatedCannotGetDeletionStatus() {
        noAuth()
                .when().get("/account-deletion/status")
                .then().statusCode(403);
    }
}

package com.demo.apitest;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * Base class for all API integration tests.
 * Configures RestAssured with the correct base URI, relaxed HTTPS validation
 * (self-signed cert), and shared helpers for authentication.
 */
public abstract class BaseApiTest {

    protected static final String BASE_URL;

    static {
        String url = System.getenv("API_BASE_URL");
        BASE_URL = (url != null && !url.isBlank()) ? url : "https://demo-proxy:8443/api";
        RestAssured.baseURI = BASE_URL;
        // Accept self-signed TLS certificate used by the proxy container.
        RestAssured.useRelaxedHTTPSValidation();
    }

    // ── Authentication helpers ─────────────────────────────────────────────

    protected static String login(String username) {
        Map<String, String> body = new HashMap<>();
        body.put("username", username);
        body.put("password", "password123");

        return given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .path("token");
    }

    /** Authenticated request spec with JSON content type. */
    protected static RequestSpecification auth(String token) {
        return given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON);
    }

    /** Unauthenticated request spec with JSON content type. */
    protected static RequestSpecification noAuth() {
        return given().contentType(ContentType.JSON);
    }

    /** Random UUID hex string for idempotency keys and unique slugs. */
    protected static String uid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}

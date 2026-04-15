package com.demo.apitest;

import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Tests: POST /appeals, GET /appeals, GET /appeals/my, GET /appeals/{id},
 *        POST /appeals/{id}/review, POST /appeals/{id}/evidence,
 *        GET /appeals/{id}/evidence
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AppealApiTest extends BaseApiTest {

    private String adminToken;
    private String memberToken;
    private String moderatorToken;

    @BeforeAll
    void setup() {
        adminToken = login("admin");
        // Register a unique member per test-class run to keep this class's
        // requests in an isolated rate-limit bucket and avoid cross-class interference.
        String unique = uid();
        java.util.Map<String, String> reg = new java.util.HashMap<>();
        reg.put("username", "appeal_" + unique);
        reg.put("email", "appeal_" + unique + "@example.com");
        reg.put("password", "password123");
        reg.put("displayName", "Appeal Test User");
        memberToken = given().contentType(io.restassured.http.ContentType.JSON).body(reg)
                .when().post("/auth/register")
                .then().statusCode(200)
                .extract().path("token");
        moderatorToken = login("moderator");
    }

    private int createAppeal(String token) {
        Map<String, Object> body = new HashMap<>();
        body.put("relatedEntityType", "PRODUCT");
        body.put("relatedEntityId", 1);
        body.put("reason", "Integration test appeal " + uid());

        return auth(token)
                .body(body)
                .when().post("/appeals")
                .then().statusCode(200)
                .body("id", notNullValue())
                .body("status", not(emptyOrNullString()))
                .extract().path("id");
    }

    @Test
    void createAppeal() {
        Map<String, Object> body = new HashMap<>();
        body.put("relatedEntityType", "PRODUCT");
        body.put("relatedEntityId", 1);
        body.put("reason", "Need review of product listing decision");

        auth(memberToken)
                .body(body)
                .when().post("/appeals")
                .then().statusCode(200)
                .body("id", notNullValue())
                .body("status", not(emptyOrNullString()));
    }

    @Test
    void getAllAppeals() {
        // Create one first so list is non-empty
        createAppeal(memberToken);

        auth(moderatorToken)
                .when().get("/appeals")
                .then().statusCode(200)
                .body("$", instanceOf(List.class));
    }

    @Test
    void getMyAppeals() {
        createAppeal(memberToken);

        auth(memberToken)
                .when().get("/appeals/my")
                .then().statusCode(200)
                .body("$", instanceOf(List.class));
    }

    @Test
    void getAppealById() {
        int id = createAppeal(memberToken);

        auth(memberToken)
                .when().get("/appeals/" + id)
                .then().statusCode(200)
                .body("id", equalTo(id))
                .body("status", not(emptyOrNullString()));
    }

    @Test
    void reviewAppeal() {
        int id = createAppeal(memberToken);

        Map<String, String> body = new HashMap<>();
        body.put("status", "APPROVED");
        body.put("reviewNotes", "Looks good, approved after review.");

        auth(moderatorToken)
                .body(body)
                .when().post("/appeals/" + id + "/review")
                .then().statusCode(200)
                .body("status", equalTo("APPROVED"));
    }

    @Test
    void uploadEvidence() {
        int id = createAppeal(memberToken);

        // Upload a minimal valid JPEG (3-byte magic + padding)
        byte[] jpegBytes = new byte[]{
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01
        };

        given()
                .header("Authorization", "Bearer " + memberToken)
                .multiPart("file", "evidence.jpg", jpegBytes, "image/jpeg")
                .when().post("/appeals/" + id + "/evidence")
                .then().statusCode(200);
    }

    @Test
    void evidenceHardCapFiveFiles() {
        int id = createAppeal(memberToken);

        byte[] jpegBytes = new byte[]{
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01
        };

        // Upload 5 files — all should succeed
        for (int i = 0; i < 5; i++) {
            given()
                    .header("Authorization", "Bearer " + memberToken)
                    .multiPart("file", "evidence" + i + ".jpg", jpegBytes, "image/jpeg")
                    .when().post("/appeals/" + id + "/evidence")
                    .then().statusCode(200);
        }

        // 6th upload must fail
        given()
                .header("Authorization", "Bearer " + memberToken)
                .multiPart("file", "evidence6.jpg", jpegBytes, "image/jpeg")
                .when().post("/appeals/" + id + "/evidence")
                .then().statusCode(anyOf(is(400), is(409)));
    }

    @Test
    void getEvidenceList() {
        int id = createAppeal(memberToken);

        // Upload one first
        byte[] jpegBytes = new byte[]{
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01
        };
        given()
                .header("Authorization", "Bearer " + memberToken)
                .multiPart("file", "proof.jpg", jpegBytes, "image/jpeg")
                .when().post("/appeals/" + id + "/evidence")
                .then().statusCode(200);

        // Verify evidence list does NOT expose storedPath
        Response evResp = auth(memberToken)
                .when().get("/appeals/" + id + "/evidence")
                .then().statusCode(200)
                .body("$", not(empty()))
                .extract().response();

        List<Object> storedPaths = evResp.jsonPath().getList("storedPath");
        if (storedPaths != null) {
            for (Object p : storedPaths) {
                assert p == null : "storedPath must not be exposed in evidence list response";
            }
        }
    }

    @Test
    void userCannotViewOtherUsersAppeal() {
        int id = createAppeal(memberToken);

        // Seller token (not the owner) should be denied
        String sellerToken = login("seller");
        auth(sellerToken)
                .when().get("/appeals/" + id)
                .then().statusCode(403);
    }

    @Test
    void createAppealMissingReasonRejected() {
        Map<String, Object> body = new HashMap<>();
        body.put("relatedEntityType", "PRODUCT");
        body.put("relatedEntityId", 1);
        // No reason field

        auth(memberToken)
                .body(body)
                .when().post("/appeals")
                .then().statusCode(400);
    }

    @Test
    void nonImageFileRejected() {
        int id = createAppeal(memberToken);

        // Upload a text file disguised as jpg
        byte[] textBytes = "this is not an image".getBytes();

        given()
                .header("Authorization", "Bearer " + memberToken)
                .multiPart("file", "fake.jpg", textBytes, "image/jpeg")
                .when().post("/appeals/" + id + "/evidence")
                .then().statusCode(400);
    }

    @Test
    void appealNotFoundReturns404() {
        auth(memberToken)
                .when().get("/appeals/99999")
                .then().statusCode(anyOf(is(403), is(404)));
    }
}

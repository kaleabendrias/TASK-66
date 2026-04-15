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
 * Tests: POST /incidents, GET /incidents, GET /incidents/my, GET /incidents/{id},
 *        POST /incidents/{id}/acknowledge, PATCH /incidents/{id}/status,
 *        POST /incidents/{id}/comments, GET /incidents/{id}/comments,
 *        GET /incidents/{id}/escalations
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IncidentApiTest extends BaseApiTest {

    private String moderatorToken;
    private String memberToken;

    @BeforeAll
    void setup() {
        moderatorToken = login("moderator");
        memberToken = login("member");
    }

    private int createIncident(String token) {
        Map<String, Object> body = new HashMap<>();
        body.put("incidentType", "ORDER_ISSUE");
        body.put("severity", "HIGH");
        body.put("title", "Test incident " + uid());
        body.put("description", "Created by integration test");
        body.put("sellerId", 3);

        return auth(token)
                .body(body)
                .when().post("/incidents")
                .then().statusCode(200)
                .body("status", equalTo("OPEN"))
                .body("id", notNullValue())
                .extract().path("id");
    }

    @Test
    void createIncidentHasSlaFields() {
        Map<String, Object> body = new HashMap<>();
        body.put("incidentType", "ORDER_ISSUE");
        body.put("severity", "HIGH");
        body.put("title", "SLA test " + uid());
        body.put("description", "Checking SLA fields");
        body.put("sellerId", 3);

        auth(moderatorToken)
                .body(body)
                .when().post("/incidents")
                .then().statusCode(200)
                .body("status", equalTo("OPEN"))
                .body("slaAckDeadline", notNullValue())
                .body("slaResolveDeadline", notNullValue());
    }

    @Test
    void listAllIncidents() {
        auth(moderatorToken)
                .when().get("/incidents")
                .then().statusCode(200)
                .body("$", instanceOf(List.class));
    }

    @Test
    void getMyIncidents() {
        // Create one first, then list mine
        createIncident(memberToken);

        auth(memberToken)
                .when().get("/incidents/my")
                .then().statusCode(200)
                .body("$", instanceOf(List.class));
    }

    @Test
    void getIncidentById() {
        int id = createIncident(moderatorToken);

        auth(moderatorToken)
                .when().get("/incidents/" + id)
                .then().statusCode(200)
                .body("id", equalTo(id));
    }

    @Test
    void acknowledgeIncident() {
        int id = createIncident(moderatorToken);

        auth(moderatorToken)
                .when().post("/incidents/" + id + "/acknowledge")
                .then().statusCode(200)
                .body("status", equalTo("ACKNOWLEDGED"));
    }

    @Test
    void acknowledgeIdempotent() {
        int id = createIncident(moderatorToken);
        auth(moderatorToken).when().post("/incidents/" + id + "/acknowledge")
                .then().statusCode(200);
        // Second call should succeed too (idempotent)
        auth(moderatorToken).when().post("/incidents/" + id + "/acknowledge")
                .then().statusCode(anyOf(is(200), is(409)));
    }

    @Test
    void resolveRequiresClosureCode() {
        int id = createIncident(moderatorToken);

        Map<String, String> ack = new HashMap<>();
        ack.put("status", "ACKNOWLEDGED");
        auth(moderatorToken).body(ack).when().patch("/incidents/" + id + "/status");

        Map<String, String> prog = new HashMap<>();
        prog.put("status", "IN_PROGRESS");
        auth(moderatorToken).body(prog).when().patch("/incidents/" + id + "/status");

        Map<String, String> resolve = new HashMap<>();
        resolve.put("status", "RESOLVED");
        auth(moderatorToken)
                .body(resolve)
                .when().patch("/incidents/" + id + "/status")
                .then().statusCode(anyOf(is(400), is(409)));
    }

    @Test
    void resolveWithClosureCodeSucceeds() {
        int id = createIncident(moderatorToken);

        Map<String, String> ack = new HashMap<>();
        ack.put("status", "ACKNOWLEDGED");
        auth(moderatorToken).body(ack).when().patch("/incidents/" + id + "/status");

        Map<String, String> prog = new HashMap<>();
        prog.put("status", "IN_PROGRESS");
        auth(moderatorToken).body(prog).when().patch("/incidents/" + id + "/status");

        Map<String, String> resolve = new HashMap<>();
        resolve.put("status", "RESOLVED");
        resolve.put("closureCode", "FIXED");

        auth(moderatorToken)
                .body(resolve)
                .when().patch("/incidents/" + id + "/status")
                .then().statusCode(200)
                .body("closureCode", equalTo("FIXED"));
    }

    @Test
    void addAndGetComment() {
        int id = createIncident(moderatorToken);

        Map<String, String> comment = new HashMap<>();
        comment.put("content", "Integration test comment");

        auth(moderatorToken)
                .body(comment)
                .when().post("/incidents/" + id + "/comments")
                .then().statusCode(200);

        auth(moderatorToken)
                .when().get("/incidents/" + id + "/comments")
                .then().statusCode(200)
                .body("$", not(empty()))
                .body("[0].content", containsString("Integration test comment"));
    }

    @Test
    void getEscalations() {
        int id = createIncident(moderatorToken);

        auth(moderatorToken)
                .when().get("/incidents/" + id + "/escalations")
                .then().statusCode(200)
                .body("$", instanceOf(List.class));
    }

    @Test
    void userCannotViewOtherUsersIncident() {
        int id = createIncident(moderatorToken);

        auth(memberToken)
                .when().get("/incidents/" + id)
                .then().statusCode(403);
    }

    @Test
    void incidentNotFoundReturns404() {
        auth(moderatorToken)
                .when().get("/incidents/99999")
                .then().statusCode(404);
    }
}

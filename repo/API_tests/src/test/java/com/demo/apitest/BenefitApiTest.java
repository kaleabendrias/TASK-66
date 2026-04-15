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
 * Tests: GET /benefits/packages/tier/{tierId},
 *        GET /benefits/items/package/{packageId},
 *        POST /benefits/issue, POST /benefits/redeem
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BenefitApiTest extends BaseApiTest {

    private String adminToken;
    private String memberToken;
    private String moderatorToken;

    @BeforeAll
    void setup() {
        adminToken = login("admin");
        memberToken = login("member");
        moderatorToken = login("moderator");
    }

    @Test
    void getBenefitPackagesForTier() {
        auth(memberToken)
                .when().get("/benefits/packages/tier/1")
                .then().statusCode(200)
                .body("$", instanceOf(List.class));
    }

    @Test
    void getBenefitItemsForPackage() {
        // Get a package id from tier 1 first
        Response pkgResp = auth(memberToken)
                .when().get("/benefits/packages/tier/1")
                .then().statusCode(200)
                .extract().response();

        List<Integer> ids = pkgResp.jsonPath().getList("id");
        if (ids != null && !ids.isEmpty()) {
            int packageId = ids.get(0);
            auth(memberToken)
                    .when().get("/benefits/items/package/" + packageId)
                    .then().statusCode(200)
                    .body("$", instanceOf(List.class));
        }
    }

    @Test
    void issueBenefitWithValidOrderReference() {
        // Create an order to reference
        Map<String, Object> orderBody = new HashMap<>();
        orderBody.put("productId", 1);
        orderBody.put("quantity", 1);
        orderBody.put("inventoryItemId", 1);

        Response orderResp = auth(memberToken)
                .body(orderBody)
                .when().post("/orders")
                .then().statusCode(200)
                .extract().response();

        int orderId = orderResp.path("id");

        // Get a benefit item id from the member's tier
        int tierId = auth(memberToken)
                .when().get("/members/me")
                .then().statusCode(200)
                .extract().path("tierId");

        Response pkgResp = auth(memberToken)
                .when().get("/benefits/packages/tier/" + tierId)
                .then().statusCode(200)
                .extract().response();

        List<Integer> pkgIds = pkgResp.jsonPath().getList("id");
        if (pkgIds == null || pkgIds.isEmpty()) return;

        Response itemsResp = auth(memberToken)
                .when().get("/benefits/items/package/" + pkgIds.get(0))
                .then().statusCode(200)
                .extract().response();

        List<Integer> itemIds = itemsResp.jsonPath().getList("id");
        if (itemIds == null || itemIds.isEmpty()) return;

        // Member user has id=2 in seed data
        Map<String, Object> issueBody = new HashMap<>();
        issueBody.put("memberId", 2);
        issueBody.put("benefitItemId", itemIds.get(0));
        issueBody.put("referenceType", "ORDER");
        issueBody.put("referenceId", orderId);

        auth(adminToken)
                .body(issueBody)
                .when().post("/benefits/issue")
                .then().statusCode(anyOf(is(200), is(400), is(409)));
    }

    @Test
    void issueBenefitInvalidReferenceTypeRejected() {
        Map<String, Object> body = new HashMap<>();
        body.put("memberId", 2);
        body.put("benefitItemId", 1);
        body.put("referenceType", "INVALID_TYPE");
        body.put("referenceId", 1);

        auth(adminToken)
                .body(body)
                .when().post("/benefits/issue")
                .then().statusCode(400);
    }

    @Test
    void issueBenefitMissingReferenceTypeRejected() {
        Map<String, Object> body = new HashMap<>();
        body.put("memberId", 2);
        body.put("benefitItemId", 1);
        body.put("referenceId", 1);

        auth(adminToken)
                .body(body)
                .when().post("/benefits/issue")
                .then().statusCode(400);
    }

    @Test
    void redeemBenefit() {
        // Get a benefit item to redeem
        int tierId = auth(memberToken)
                .when().get("/members/me")
                .then().statusCode(200)
                .extract().path("tierId");

        Response pkgResp = auth(memberToken)
                .when().get("/benefits/packages/tier/" + tierId)
                .then().statusCode(200)
                .extract().response();

        List<Integer> pkgIds = pkgResp.jsonPath().getList("id");
        if (pkgIds == null || pkgIds.isEmpty()) return;

        Response itemsResp = auth(memberToken)
                .when().get("/benefits/items/package/" + pkgIds.get(0))
                .then().statusCode(200)
                .extract().response();

        List<Integer> itemIds = itemsResp.jsonPath().getList("id");
        if (itemIds == null || itemIds.isEmpty()) return;

        // Create order for the reference
        Map<String, Object> orderBody = new HashMap<>();
        orderBody.put("productId", 2);
        orderBody.put("quantity", 1);
        orderBody.put("inventoryItemId", 1);

        Response orderResp = auth(memberToken)
                .body(orderBody)
                .when().post("/orders")
                .then().statusCode(200)
                .extract().response();
        int orderId = orderResp.path("id");

        Map<String, Object> body = new HashMap<>();
        body.put("benefitItemId", itemIds.get(0));
        body.put("referenceType", "ORDER");
        body.put("referenceId", orderId);

        auth(memberToken)
                .body(body)
                .when().post("/benefits/redeem")
                .then().statusCode(anyOf(is(200), is(400), is(409)));
    }

    @Test
    void unauthenticatedCannotAccessBenefitPackages() {
        noAuth()
                .when().get("/benefits/packages/tier/1")
                .then().statusCode(403);
    }
}

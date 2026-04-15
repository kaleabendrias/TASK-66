package com.demo.apitest;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;

/**
 * Tests: GET /tiers, GET /tiers/{id},
 *        GET /members/me, PUT /members/me/phone,
 *        POST /members/me/spend, GET /members/me/spend/history,
 *        GET /members/{userId}
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MemberApiTest extends BaseApiTest {

    private String adminToken;
    private String memberToken;
    private String moderatorToken;

    @BeforeAll
    void setup() {
        adminToken = login("admin");
        memberToken = login("member");
        moderatorToken = login("moderator");
    }

    // ── Tiers ──────────────────────────────────────────────────────────────

    @Test
    void getAllTiers() {
        noAuth()
                .when().get("/tiers")
                .then().statusCode(200)
                .body("$", not(empty()))
                .body("size()", equalTo(3));
    }

    @Test
    void getTierById() {
        noAuth()
                .when().get("/tiers/1")
                .then().statusCode(200)
                .body("id", equalTo(1));
    }

    @Test
    void tiersHaveCorrectSpendBands() {
        // Bronze: 0-499, Silver: 500-1499, Gold: 1500+
        noAuth()
                .when().get("/tiers")
                .then().statusCode(200)
                .body("find { it.name == 'Bronze' }.minSpend", equalTo(0))
                .body("find { it.name == 'Silver' }.minSpend", equalTo(500))
                .body("find { it.name == 'Gold' }.minSpend", equalTo(1500));
    }

    // ── Member profile ─────────────────────────────────────────────────────

    @Test
    void getMemberProfile() {
        auth(memberToken)
                .when().get("/members/me")
                .then().statusCode(200)
                .body("tierId", notNullValue())
                .body("tierName", not(emptyOrNullString()));
    }

    @Test
    void getMemberProfileAdminView() {
        // Admin gets member profile by userId (member user has id=2 in seed)
        auth(adminToken)
                .when().get("/members/2")
                .then().statusCode(200)
                .body("userId", equalTo(2));
    }

    @Test
    void getMemberProfileByModeratorHasAccess() {
        auth(moderatorToken)
                .when().get("/members/2")
                .then().statusCode(200);
    }

    @Test
    void updateMemberPhone() {
        Map<String, String> body = new HashMap<>();
        body.put("phone", "555-123-4567");

        auth(memberToken)
                .body(body)
                .when().put("/members/me/phone")
                .then().statusCode(200);
    }

    @Test
    void adjustMemberSpend() {
        // Only MODERATOR or ADMINISTRATOR can adjust spend
        Map<String, Object> body = new HashMap<>();
        body.put("amount", 100);
        body.put("reference", "TEST-ADJ-" + uid());

        auth(adminToken)
                .body(body)
                .when().post("/members/me/spend")
                .then().statusCode(200);
    }

    @Test
    void getSpendHistory() {
        auth(memberToken)
                .when().get("/members/me/spend/history")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void memberCannotViewOtherMemberProfile() {
        auth(memberToken)
                .when().get("/members/1")
                .then().statusCode(403);
    }

    @Test
    void unauthenticatedCannotViewMemberProfile() {
        noAuth()
                .when().get("/members/me")
                .then().statusCode(403);
    }
}

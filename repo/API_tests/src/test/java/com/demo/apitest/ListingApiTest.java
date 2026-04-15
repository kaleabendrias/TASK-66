package com.demo.apitest;

import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;

/**
 * Tests: GET /listings, GET /listings/slug/{slug}, GET /listings/search,
 *        POST /listings, PUT /listings/{id},
 *        POST /listings/{id}/publish, POST /listings/{id}/archive
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ListingApiTest extends BaseApiTest {

    private String adminToken;
    private String sellerToken;
    private String moderatorToken;

    @BeforeAll
    void setup() {
        adminToken = login("admin");
        sellerToken = login("seller");
        moderatorToken = login("moderator");
    }

    @Test
    void getPublishedListings() {
        noAuth()
                .when().get("/listings")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void getListingBySlug() {
        // Get a slug from the published listing list
        Response listResp = noAuth()
                .when().get("/listings")
                .then().statusCode(200)
                .extract().response();

        java.util.List<String> slugs = listResp.jsonPath().getList("slug");
        if (slugs != null && !slugs.isEmpty()) {
            String slug = slugs.get(0);
            noAuth()
                    .when().get("/listings/slug/" + slug)
                    .then().statusCode(200)
                    .body("slug", equalTo(slug))
                    .body("title", not(emptyOrNullString()));
        }
    }

    @Test
    void searchListingsKeyword() {
        noAuth()
                .when().get("/listings/search?q=pottery")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void searchListingsNeighborhood() {
        noAuth()
                .when().get("/listings/search?neighborhood=Downtown")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void searchListingsPriceRange() {
        noAuth()
                .when().get("/listings/search?minPrice=0&maxPrice=500")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void searchListingsGeoRadius() {
        noAuth()
                .when().get("/listings/search?lat=40.73&lng=-73.99&radiusMiles=10")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void searchListingsDateWindow() {
        noAuth()
                .when().get("/listings/search?availableAfter=2026-01-01&availableBefore=2026-12-31")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    @Test
    void createListing() {
        // Use seeded product id=1 which belongs to seller
        String unique = uid();
        Map<String, Object> body = new HashMap<>();
        body.put("productId", 1);
        body.put("title", "Test Listing " + unique);
        body.put("slug", "test-listing-" + unique);
        body.put("summary", "A test listing from integration test");
        body.put("neighborhood", "Test District");
        body.put("price", 49.99);

        auth(sellerToken)
                .body(body)
                .when().post("/listings")
                .then().statusCode(200)
                .body("slug", equalTo("test-listing-" + unique))
                .body("title", containsString("Test Listing"))
                .body("id", notNullValue());
    }

    @Test
    void updateListing() {
        String unique = uid();
        Map<String, Object> create = new HashMap<>();
        create.put("productId", 1);
        create.put("title", "Listing Before " + unique);
        create.put("slug", "lst-before-" + unique);
        create.put("summary", "Before update");
        create.put("neighborhood", "Old District");
        create.put("price", 25.00);

        int id = auth(sellerToken)
                .body(create)
                .when().post("/listings")
                .then().statusCode(200)
                .extract().path("id");

        Map<String, Object> update = new HashMap<>();
        update.put("productId", 1);
        update.put("title", "Listing After " + unique);
        update.put("slug", "lst-after-" + unique);
        update.put("summary", "After update");
        update.put("neighborhood", "New District");
        update.put("price", 30.00);

        auth(sellerToken)
                .body(update)
                .when().put("/listings/" + id)
                .then().statusCode(200);
    }

    @Test
    void publishListing() {
        // Create a listing (DRAFT), then publish it as moderator
        String unique = uid();
        Map<String, Object> create = new HashMap<>();
        create.put("productId", 1);
        create.put("title", "Publish Me " + unique);
        create.put("slug", "pub-" + unique);
        create.put("summary", "Will be published");
        create.put("price", 20.00);

        int id = auth(sellerToken)
                .body(create)
                .when().post("/listings")
                .then().statusCode(200)
                .extract().path("id");

        auth(moderatorToken)
                .when().post("/listings/" + id + "/publish")
                .then().statusCode(200);
    }

    @Test
    void archiveListing() {
        // Create and publish, then archive
        String unique = uid();
        Map<String, Object> create = new HashMap<>();
        create.put("productId", 1);
        create.put("title", "Archive Me " + unique);
        create.put("slug", "arc-" + unique);
        create.put("summary", "Will be archived");
        create.put("price", 15.00);

        int id = auth(sellerToken)
                .body(create)
                .when().post("/listings")
                .then().statusCode(200)
                .extract().path("id");

        // Publish first (must be PUBLISHED to archive in typical state machine)
        auth(moderatorToken)
                .when().post("/listings/" + id + "/publish")
                .then().statusCode(200);

        auth(moderatorToken)
                .when().post("/listings/" + id + "/archive")
                .then().statusCode(200);
    }

    @Test
    void memberCannotCreateListing() {
        String memberToken = login("member");
        Map<String, Object> body = new HashMap<>();
        body.put("productId", 1);
        body.put("title", "Member Listing Attempt");
        body.put("slug", "member-lst-" + uid());
        body.put("summary", "Should fail");
        body.put("price", 5.00);

        auth(memberToken)
                .body(body)
                .when().post("/listings")
                .then().statusCode(403);
    }
}

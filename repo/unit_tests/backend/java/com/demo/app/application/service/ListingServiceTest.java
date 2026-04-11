package com.demo.app.application.service;

import com.demo.app.DemoApplication;
import com.demo.app.TestFixtures;
import com.demo.app.domain.enums.Role;
import com.demo.app.domain.model.Listing;
import com.demo.app.persistence.entity.CategoryEntity;
import com.demo.app.persistence.entity.ProductEntity;
import com.demo.app.persistence.entity.UserEntity;
import com.demo.app.persistence.repository.CategoryRepository;
import com.demo.app.persistence.repository.ListingRepository;
import com.demo.app.persistence.repository.ProductRepository;
import com.demo.app.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = DemoApplication.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("ListingService - listing CRUD with discovery fields")
class ListingServiceTest {

    @Autowired private ListingService listingService;
    @Autowired private ListingRepository listingRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;

    private ProductEntity product;

    @BeforeEach
    void setUp() {
        UserEntity seller = userRepository.save(TestFixtures.user("listseller", Role.SELLER));
        CategoryEntity category = categoryRepository.save(TestFixtures.category("Housing"));
        product = productRepository.save(TestFixtures.product("Loft", new BigDecimal("1500.00"), category, seller));
    }

    private Listing buildListing(String slug) {
        return Listing.builder()
                .productId(product.getId())
                .title("Charming loft in the arts district")
                .slug(slug)
                .summary("Great natural light, walking distance to the park")
                .tags(List.of("loft", "downtown"))
                .featured(false)
                .neighborhood("Arts District")
                .latitude(40.7128)
                .longitude(-74.0060)
                .price(new BigDecimal("2400.00"))
                .sqft(950)
                .layout("2BR/1BA")
                .availableFrom(LocalDate.of(2026, 5, 1))
                .availableTo(LocalDate.of(2026, 12, 31))
                .build();
    }

    @Test
    @DisplayName("create persists all discovery fields")
    void testCreate_persistsDiscoveryFields() {
        Listing created = listingService.create(buildListing("loft-arts-1"));

        assertNotNull(created.getId());
        Listing reloaded = listingRepository.findById(created.getId()).orElseThrow().toModel();
        assertEquals("Arts District", reloaded.getNeighborhood());
        assertEquals(40.7128, reloaded.getLatitude());
        assertEquals(-74.0060, reloaded.getLongitude());
        assertEquals(0, new BigDecimal("2400.00").compareTo(reloaded.getPrice()));
        assertEquals(950, reloaded.getSqft());
        assertEquals("2BR/1BA", reloaded.getLayout());
        assertEquals(LocalDate.of(2026, 5, 1), reloaded.getAvailableFrom());
        assertEquals(LocalDate.of(2026, 12, 31), reloaded.getAvailableTo());
    }

    @Test
    @DisplayName("create rejects duplicate slugs")
    void testCreate_duplicateSlug_throws() {
        listingService.create(buildListing("dup-slug"));
        assertThrows(RuntimeException.class, () -> listingService.create(buildListing("dup-slug")));
    }

    @Test
    @DisplayName("update overwrites discovery fields")
    void testUpdate_overwritesDiscoveryFields() {
        Listing original = listingService.create(buildListing("loft-update"));

        Listing patch = Listing.builder()
                .productId(product.getId())
                .title("Updated title")
                .slug("loft-update")
                .summary("Updated summary")
                .tags(List.of("updated"))
                .featured(true)
                .neighborhood("Midtown")
                .latitude(34.05)
                .longitude(-118.25)
                .price(new BigDecimal("3000.00"))
                .sqft(1100)
                .layout("3BR/2BA")
                .availableFrom(LocalDate.of(2026, 6, 1))
                .availableTo(LocalDate.of(2027, 6, 1))
                .build();
        Listing updated = listingService.update(original.getId(), patch);

        assertEquals("Midtown", updated.getNeighborhood());
        assertEquals(34.05, updated.getLatitude());
        assertEquals(1100, updated.getSqft());
        assertEquals("3BR/2BA", updated.getLayout());
        assertEquals(LocalDate.of(2027, 6, 1), updated.getAvailableTo());
    }

    @Test
    @DisplayName("publish flips status to PUBLISHED and stamps publishedAt")
    void testPublish() {
        Listing created = listingService.create(buildListing("loft-publish"));
        Listing published = listingService.publish(created.getId());
        assertEquals("PUBLISHED", published.getStatus().name());
        assertNotNull(published.getPublishedAt());
    }

    @Test
    @DisplayName("incrementViewCount bumps both viewCount and weeklyViews")
    void testIncrementViewCount() {
        Listing created = listingService.create(buildListing("loft-views"));
        listingService.incrementViewCount(created.getId());
        listingService.incrementViewCount(created.getId());
        Listing reloaded = listingRepository.findById(created.getId()).orElseThrow().toModel();
        assertEquals(2, reloaded.getViewCount());
        assertEquals(2, reloaded.getWeeklyViews());
    }

    @Test
    @DisplayName("searchAdvanced filters by price range and neighborhood")
    void testSearchAdvanced_priceAndNeighborhood() {
        Listing l1 = listingService.create(buildListing("loft-search-1"));
        listingService.publish(l1.getId());

        // A listing in a different neighborhood should be filtered out.
        Listing other = Listing.builder()
                .productId(product.getId())
                .title("Tiny studio")
                .slug("studio-other")
                .summary("compact")
                .tags(List.of("studio"))
                .featured(false)
                .neighborhood("Suburbs")
                .price(new BigDecimal("1000.00"))
                .sqft(400)
                .build();
        Listing l2 = listingService.create(other);
        listingService.publish(l2.getId());

        List<Listing> hits = listingService.searchAdvanced(
                null, "Arts District", null, null, null, null, null,
                new BigDecimal("2000.00"), new BigDecimal("2500.00"),
                null, null, null);
        assertTrue(hits.stream().anyMatch(l -> "loft-search-1".equals(l.getSlug())));
        assertTrue(hits.stream().noneMatch(l -> "studio-other".equals(l.getSlug())));
    }
}

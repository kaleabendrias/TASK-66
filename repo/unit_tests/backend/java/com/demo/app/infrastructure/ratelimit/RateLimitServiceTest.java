package com.demo.app.infrastructure.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RateLimitService - Token bucket rate limiting")
class RateLimitServiceTest {

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService(60);
    }

    @Test
    @DisplayName("Consuming within limit returns true for all requests")
    void testTryConsume_withinLimit_returnsTrue() {
        String key = "user-1";

        for (int i = 0; i < 60; i++) {
            assertTrue(rateLimitService.tryConsume(key),
                    "Request " + (i + 1) + " should be allowed");
        }
    }

    @Test
    @DisplayName("Exceeding limit returns false on the 61st request")
    void testTryConsume_exceedsLimit_returnsFalse() {
        String key = "user-2";

        for (int i = 0; i < 60; i++) {
            rateLimitService.tryConsume(key);
        }

        assertFalse(rateLimitService.tryConsume(key),
                "61st request should be rejected");
    }

    @Test
    @DisplayName("Different keys have independent rate limits")
    void testDifferentKeys_independentLimits() {
        String key1 = "user-a";
        String key2 = "user-b";

        for (int i = 0; i < 60; i++) {
            rateLimitService.tryConsume(key1);
        }

        assertFalse(rateLimitService.tryConsume(key1),
                "key1 should be exhausted");
        assertTrue(rateLimitService.tryConsume(key2),
                "key2 should still have capacity");
    }

    @Test
    @DisplayName("evictExpired clears all buckets, allowing fresh consumption")
    void testEvictExpired_clearsAllBuckets() {
        String key = "user-evict";

        for (int i = 0; i < 60; i++) {
            rateLimitService.tryConsume(key);
        }
        assertFalse(rateLimitService.tryConsume(key));

        rateLimitService.evictExpired();

        assertTrue(rateLimitService.tryConsume(key),
                "After eviction, bucket should be fresh");
    }
}

package com.demo.app.application.service;

import com.demo.app.DemoApplication;
import com.demo.app.TestFixtures;
import com.demo.app.domain.enums.Role;
import com.demo.app.persistence.entity.RiskEventEntity;
import com.demo.app.persistence.entity.RiskScoreEntity;
import com.demo.app.persistence.entity.UserEntity;
import com.demo.app.persistence.repository.RiskEventRepository;
import com.demo.app.persistence.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = DemoApplication.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("RiskAnalyticsService")
class RiskAnalyticsServiceTest {
    @Autowired private RiskAnalyticsService riskAnalyticsService;
    @Autowired private UserRepository userRepository;
    @Autowired private RiskEventRepository riskEventRepository;

    private UserEntity user;

    @BeforeEach void setUp() { user = userRepository.save(TestFixtures.user("riskuser", Role.SELLER)); }

    @Test void testRecordEvent() {
        RiskEventEntity e = riskAnalyticsService.recordEvent(user.getId(), "TEST", "LOW", Map.of("k", "v"));
        assertNotNull(e.getId());
        assertEquals("TEST", e.getEventType());
    }

    @Test void testComputeScore_noEvents() {
        RiskScoreEntity s = riskAnalyticsService.computeScore(user.getId());
        assertNotNull(s);
        assertEquals(0.0, s.getScore());
    }

    @Test void testComputeScore_withEvents() {
        riskAnalyticsService.recordEvent(user.getId(), "MISSED_PICKUP_CHECKIN", "HIGH", null);
        riskAnalyticsService.recordEvent(user.getId(), "BUDDY_PUNCHING", "HIGH", null);
        RiskScoreEntity s = riskAnalyticsService.computeScore(user.getId());
        assertTrue(s.getScore() > 0);
    }

    @Test void testGetScore_null() { assertNull(riskAnalyticsService.getScore(user.getId())); }
    @Test void testGetScore_afterCompute() {
        riskAnalyticsService.computeScore(user.getId());
        assertNotNull(riskAnalyticsService.getScore(user.getId()));
    }

    @Test void testGetHighRiskUsers() { assertNotNull(riskAnalyticsService.getHighRiskUsers(50)); }
    @Test void testGetEventsByUser() { assertNotNull(riskAnalyticsService.getEventsByUser(user.getId())); }
}

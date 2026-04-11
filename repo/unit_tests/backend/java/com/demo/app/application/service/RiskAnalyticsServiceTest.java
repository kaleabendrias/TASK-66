package com.demo.app.application.service;

import com.demo.app.DemoApplication;
import com.demo.app.TestFixtures;
import com.demo.app.domain.enums.Role;
import com.demo.app.persistence.entity.IncidentEntity;
import com.demo.app.persistence.entity.RiskEventEntity;
import com.demo.app.persistence.entity.RiskScoreEntity;
import com.demo.app.persistence.entity.UserEntity;
import com.demo.app.persistence.repository.IncidentRepository;
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
    @Autowired private IncidentRepository incidentRepository;

    private UserEntity user;

    @BeforeEach void setUp() { user = userRepository.save(TestFixtures.user("riskuser", Role.SELLER)); }

    private IncidentEntity sellerIncident(Long sellerId, Long reporterId, String severity,
                                          int escalationLevel, LocalDateTime createdAt, String status) {
        return incidentRepository.save(IncidentEntity.builder()
                .reporterId(reporterId)
                .sellerId(sellerId)
                .incidentType("PRODUCT_DEFECT")
                .severity(severity)
                .title("test")
                .description("test")
                .status(status)
                .escalationLevel(escalationLevel)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build());
    }

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

    @Test
    @DisplayName("score is driven by incidents linked to the user as seller, not as reporter/assignee")
    void testComputeScore_sellerScopedWindow() {
        UserEntity buyer = userRepository.save(TestFixtures.user("riskbuyer", Role.MEMBER));
        UserEntity otherSeller = userRepository.save(TestFixtures.user("riskother", Role.SELLER));

        LocalDateTime now = LocalDateTime.now();
        // Incidents *against* our seller (count toward score):
        sellerIncident(user.getId(), buyer.getId(), "HIGH", 1, now.minusDays(2), "OPEN");
        sellerIncident(user.getId(), buyer.getId(), "HIGH", 0, now.minusDays(5), "OPEN");
        sellerIncident(user.getId(), buyer.getId(), "NORMAL", 0, now.minusDays(10), "ACKNOWLEDGED");
        sellerIncident(user.getId(), buyer.getId(), "NORMAL", 0, now.minusDays(15), "RESOLVED");

        // Incidents *reported by* our seller against someone else — must NOT count:
        sellerIncident(otherSeller.getId(), user.getId(), "EMERGENCY", 1, now.minusDays(1), "OPEN");
        sellerIncident(otherSeller.getId(), user.getId(), "HIGH", 1, now.minusDays(3), "OPEN");

        // Incident outside the 30-day window — must NOT count:
        sellerIncident(user.getId(), buyer.getId(), "EMERGENCY", 1, now.minusDays(45), "OPEN");

        RiskScoreEntity score = riskAnalyticsService.computeScore(user.getId());

        assertEquals(4, score.getSellerComplaintCount());
        assertEquals(3, score.getOpenIncidentCount());
        assertTrue(score.getFactors().contains("seller_escalated_incidents_30d"));
        assertTrue(score.getFactors().contains("seller_total_incidents_30d"));
        // 1*15 (escalated) + 2*12 (high severity) + 2*8 (repeat) = 55
        assertEquals(55.0, score.getScore());
    }

    @Test
    @DisplayName("incidents reported by the user but against other sellers do not inflate their score")
    void testComputeScore_reportedByUserDoesNotCount() {
        UserEntity seller2 = userRepository.save(TestFixtures.user("risksel2", Role.SELLER));
        LocalDateTime now = LocalDateTime.now();
        sellerIncident(seller2.getId(), user.getId(), "EMERGENCY", 1, now.minusDays(1), "OPEN");
        sellerIncident(seller2.getId(), user.getId(), "HIGH", 1, now.minusDays(2), "OPEN");

        RiskScoreEntity score = riskAnalyticsService.computeScore(user.getId());
        assertEquals(0, score.getSellerComplaintCount());
        assertEquals(0.0, score.getScore());
    }
}

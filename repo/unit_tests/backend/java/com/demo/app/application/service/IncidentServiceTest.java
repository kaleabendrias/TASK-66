package com.demo.app.application.service;

import com.demo.app.DemoApplication;
import com.demo.app.TestFixtures;
import com.demo.app.domain.enums.Role;
import com.demo.app.domain.model.Incident;
import com.demo.app.domain.model.IncidentComment;
import com.demo.app.persistence.entity.UserEntity;
import com.demo.app.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = DemoApplication.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("IncidentService - Incident lifecycle, comments, and SLA management")
class IncidentServiceTest {

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private UserRepository userRepository;

    private UserEntity reporter;

    @BeforeEach
    void setUp() {
        reporter = userRepository.save(TestFixtures.user("reporter", Role.MEMBER));
    }

    @Test
    @DisplayName("create sets OPEN status and slaAckDeadline ~15 minutes from now")
    void testCreate_setsOpenStatusAndSla() {
        LocalDateTime before = LocalDateTime.now();

        Incident incident = incidentService.create(
                reporter.getId(), "FRAUD", "HIGH", "Suspicious activity", "Details here", null, null, null);

        assertEquals("OPEN", incident.getStatus());
        assertNotNull(incident.getSlaAckDeadline());

        LocalDateTime expectedMin = before.plusMinutes(14);
        LocalDateTime expectedMax = LocalDateTime.now().plusMinutes(16);
        assertTrue(incident.getSlaAckDeadline().isAfter(expectedMin));
        assertTrue(incident.getSlaAckDeadline().isBefore(expectedMax));
    }

    @Test
    @DisplayName("create with EMERGENCY severity halves the SLA to ~7.5 minutes")
    void testCreate_emergencySeverity_halvesSla() {
        LocalDateTime before = LocalDateTime.now();

        Incident incident = incidentService.create(
                reporter.getId(), "FRAUD", "EMERGENCY", "Critical issue", "Urgent", null, null, null);

        assertNotNull(incident.getSlaAckDeadline());

        // 15 / 2 = 7 minutes (integer division)
        LocalDateTime expectedMin = before.plusMinutes(6);
        LocalDateTime expectedMax = LocalDateTime.now().plusMinutes(9);
        assertTrue(incident.getSlaAckDeadline().isAfter(expectedMin));
        assertTrue(incident.getSlaAckDeadline().isBefore(expectedMax));
    }

    @Test
    @DisplayName("acknowledge updates status and sets assignee")
    void testAcknowledge_updatesStatusAndAssignee() {
        Incident incident = incidentService.create(
                reporter.getId(), "FRAUD", "HIGH", "Test", "Desc", null, null, null);

        UserEntity assignee = userRepository.save(TestFixtures.user("assignee", Role.MODERATOR));
        Incident acknowledged = incidentService.acknowledge(incident.getId(), assignee.getId());

        assertEquals("ACKNOWLEDGED", acknowledged.getStatus());
        assertEquals(assignee.getId(), acknowledged.getAssigneeId());
    }

    @Test
    @DisplayName("acknowledge on already acknowledged incident is idempotent")
    void testAcknowledge_alreadyAcknowledged_idempotent() {
        Incident incident = incidentService.create(
                reporter.getId(), "FRAUD", "HIGH", "Test", "Desc", null, null, null);

        UserEntity assignee = userRepository.save(TestFixtures.user("assignee2", Role.MODERATOR));
        incidentService.acknowledge(incident.getId(), assignee.getId());
        Incident second = incidentService.acknowledge(incident.getId(), assignee.getId());

        assertEquals("ACKNOWLEDGED", second.getStatus());
    }

    @Test
    @DisplayName("updateStatus allows forward transition from OPEN to ACKNOWLEDGED")
    void testUpdateStatus_forwardOnly_open_to_acknowledged() {
        Incident incident = incidentService.create(
                reporter.getId(), "FRAUD", "HIGH", "Test", "Desc", null, null, null);

        Incident updated = incidentService.updateStatus(incident.getId(), "ACKNOWLEDGED", null);

        assertEquals("ACKNOWLEDGED", updated.getStatus());
    }

    @Test
    @DisplayName("updateStatus throws on backward transition from RESOLVED to OPEN")
    void testUpdateStatus_cannotGoBackward_throws() {
        Incident incident = incidentService.create(
                reporter.getId(), "FRAUD", "HIGH", "Test", "Desc", null, null, null);

        // Move forward to RESOLVED (closure code required for RESOLVED)
        incidentService.updateStatus(incident.getId(), "ACKNOWLEDGED", null);
        incidentService.updateStatus(incident.getId(), "IN_PROGRESS", null);
        incidentService.updateStatus(incident.getId(), "RESOLVED", "FIXED");

        assertThrows(IllegalStateException.class,
                () -> incidentService.updateStatus(incident.getId(), "OPEN", null));
    }

    @Test
    @DisplayName("addComment creates a comment on the incident")
    void testAddComment_createsComment() {
        Incident incident = incidentService.create(
                reporter.getId(), "FRAUD", "HIGH", "Test", "Desc", null, null, null);

        IncidentComment comment = incidentService.addComment(
                incident.getId(), reporter.getId(), "This is a comment");

        assertNotNull(comment);
        assertEquals(incident.getId(), comment.getIncidentId());
        assertEquals("This is a comment", comment.getContent());
    }

    @Test
    @DisplayName("getComments returns comments sorted by date ascending")
    void testGetComments_returnsSortedByDate() {
        Incident incident = incidentService.create(
                reporter.getId(), "FRAUD", "HIGH", "Test", "Desc", null, null, null);

        incidentService.addComment(incident.getId(), reporter.getId(), "First comment");
        incidentService.addComment(incident.getId(), reporter.getId(), "Second comment");
        incidentService.addComment(incident.getId(), reporter.getId(), "Third comment");

        List<IncidentComment> comments = incidentService.getComments(incident.getId());

        assertEquals(3, comments.size());
        assertEquals("First comment", comments.get(0).getContent());
        assertEquals("Second comment", comments.get(1).getContent());
        assertEquals("Third comment", comments.get(2).getContent());
    }

    @Test
    @DisplayName("create rejects sellerId pointing at a non-SELLER user (risk analytics integrity)")
    void testCreate_sellerIdMustBeSellerRole() {
        UserEntity moderator = userRepository.save(TestFixtures.user("inc_mod_for_seller_check", Role.MODERATOR));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> incidentService.create(reporter.getId(), "ORDER_ISSUE", "NORMAL",
                        "Bad seller id", "desc", null, null, moderator.getId()));
        assertTrue(ex.getMessage().contains("SELLER"), "should mention SELLER in error: " + ex.getMessage());
    }

    @Test
    @DisplayName("create accepts sellerId pointing at a SELLER user")
    void testCreate_sellerIdAcceptsSellerRole() {
        UserEntity seller = userRepository.save(TestFixtures.user("inc_real_seller", Role.SELLER));
        Incident incident = incidentService.create(
                reporter.getId(), "ORDER_ISSUE", "NORMAL", "Good seller", "desc", null, null, seller.getId());
        assertEquals(seller.getId(), incident.getSellerId());
    }

    @Test
    @DisplayName("create rejects sellerId that does not exist")
    void testCreate_sellerIdMustExist() {
        assertThrows(IllegalArgumentException.class,
                () -> incidentService.create(reporter.getId(), "ORDER_ISSUE", "NORMAL",
                        "Ghost seller", "desc", null, null, 9_999_999L));
    }
}

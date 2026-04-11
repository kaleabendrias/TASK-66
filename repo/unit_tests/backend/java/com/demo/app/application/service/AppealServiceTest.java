package com.demo.app.application.service;

import com.demo.app.DemoApplication;
import com.demo.app.TestFixtures;
import com.demo.app.domain.enums.Role;
import com.demo.app.domain.exception.ResourceNotFoundException;
import com.demo.app.domain.model.Appeal;
import com.demo.app.persistence.entity.AppealEntity;
import com.demo.app.persistence.entity.IncidentEntity;
import com.demo.app.persistence.entity.UserEntity;
import com.demo.app.persistence.repository.AppealRepository;
import com.demo.app.persistence.repository.IncidentRepository;
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
@DisplayName("AppealService - Appeal submission, review, and state transitions")
class AppealServiceTest {

    @Autowired
    private AppealService appealService;

    @Autowired
    private AppealRepository appealRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    private UserEntity user;
    private UserEntity reviewer;
    private Long incidentId1;
    private Long incidentId2;
    private Long incidentId3;

    @BeforeEach
    void setUp() {
        user = userRepository.save(TestFixtures.user("appealuser", Role.MEMBER));
        reviewer = userRepository.save(TestFixtures.user("appealreviewer", Role.MODERATOR));
        incidentId1 = createIncident().getId();
        incidentId2 = createIncident().getId();
        incidentId3 = createIncident().getId();
    }

    private IncidentEntity createIncident() {
        return incidentRepository.save(IncidentEntity.builder()
                .reporterId(user.getId())
                .incidentType("ORDER_ISSUE")
                .severity("NORMAL")
                .title("t")
                .description("d")
                .status("OPEN")
                .escalationLevel(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
    }

    @Test
    @DisplayName("create sets SUBMITTED status")
    void testCreate_setsSubmittedStatus() {
        Appeal appeal = appealService.create(user.getId(), "INCIDENT", incidentId1, "Unfair cancellation");

        assertNotNull(appeal.getId());
        assertEquals("SUBMITTED", appeal.getStatus());
        assertEquals(user.getId(), appeal.getUserId());
    }

    @Test
    @DisplayName("review as APPROVED sets resolvedAt")
    void testReview_approve_setsResolvedAt() {
        Appeal appeal = appealService.create(user.getId(), "INCIDENT", incidentId1, "Reason");

        Appeal reviewed = appealService.review(appeal.getId(), reviewer.getId(), "APPROVED", "Looks valid");

        assertEquals("APPROVED", reviewed.getStatus());
        assertNotNull(reviewed.getResolvedAt());
    }

    @Test
    @DisplayName("review as REJECTED sets resolvedAt")
    void testReview_reject_setsResolvedAt() {
        Appeal appeal = appealService.create(user.getId(), "INCIDENT", incidentId1, "Reason");

        Appeal reviewed = appealService.review(appeal.getId(), reviewer.getId(), "REJECTED", "Not valid");

        assertEquals("REJECTED", reviewed.getStatus());
        assertNotNull(reviewed.getResolvedAt());
    }

    @Test
    @DisplayName("review on already resolved appeal throws")
    void testReview_alreadyResolved_throws() {
        Appeal appeal = appealService.create(user.getId(), "INCIDENT", incidentId1, "Reason");
        appealService.review(appeal.getId(), reviewer.getId(), "APPROVED", "OK");

        assertThrows(IllegalStateException.class,
                () -> appealService.review(appeal.getId(), reviewer.getId(), "REJECTED", "Changed mind"));
    }

    @Test
    @DisplayName("review from UNDER_REVIEW status is allowed")
    void testReview_underReview_allowsReview() {
        Appeal appeal = appealService.create(user.getId(), "INCIDENT", incidentId1, "Reason");

        // Manually set to UNDER_REVIEW
        AppealEntity entity = appealRepository.findById(appeal.getId()).orElseThrow();
        entity.setStatus("UNDER_REVIEW");
        appealRepository.save(entity);

        Appeal reviewed = appealService.review(appeal.getId(), reviewer.getId(), "APPROVED", "Reviewed");

        assertEquals("APPROVED", reviewed.getStatus());
        assertNotNull(reviewed.getResolvedAt());
    }

    @Test
    @DisplayName("getPending returns only SUBMITTED appeals")
    void testGetPending_returnsOnlySubmitted() {
        appealService.create(user.getId(), "INCIDENT", incidentId1, "Pending 1");
        appealService.create(user.getId(), "INCIDENT", incidentId2, "Pending 2");

        Appeal toApprove = appealService.create(user.getId(), "INCIDENT", incidentId3, "Will be approved");
        appealService.review(toApprove.getId(), reviewer.getId(), "APPROVED", "OK");

        List<Appeal> pending = appealService.getPending();

        assertEquals(2, pending.size());
        assertTrue(pending.stream().allMatch(a -> "SUBMITTED".equals(a.getStatus())));
    }

    @Test
    @DisplayName("Transition from SUBMITTED to APPROVED is valid")
    void testTransition_submitted_to_approved_valid() {
        Appeal appeal = appealService.create(user.getId(), "INCIDENT", incidentId1, "Reason");

        Appeal reviewed = appealService.review(appeal.getId(), reviewer.getId(), "APPROVED", "Valid");

        assertEquals("APPROVED", reviewed.getStatus());
    }

    @Test
    @DisplayName("Transition from SUBMITTED to REJECTED is valid")
    void testTransition_submitted_to_rejected_valid() {
        Appeal appeal = appealService.create(user.getId(), "INCIDENT", incidentId1, "Reason");

        Appeal reviewed = appealService.review(appeal.getId(), reviewer.getId(), "REJECTED", "Invalid");

        assertEquals("REJECTED", reviewed.getStatus());
    }

    @Test
    @DisplayName("create rejects an unknown relatedEntityType")
    void testCreate_unknownRelatedEntityType_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> appealService.create(user.getId(), "WIDGET", incidentId1, "reason"));
    }

    @Test
    @DisplayName("create rejects a relatedEntityId that does not exist in the target table")
    void testCreate_missingRelatedEntityId_throws() {
        Long bogusId = 99_999_999L;
        assertThrows(ResourceNotFoundException.class,
                () -> appealService.create(user.getId(), "INCIDENT", bogusId, "reason"));
    }

    @Test
    @DisplayName("create rejects null relatedEntityId without hitting the database")
    void testCreate_nullRelatedEntityId_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> appealService.create(user.getId(), "INCIDENT", null, "reason"));
    }

    @Test
    @DisplayName("create normalizes relatedEntityType to upper case before lookup")
    void testCreate_normalizesRelatedEntityType() {
        Appeal appeal = appealService.create(user.getId(), "incident", incidentId1, "reason");
        assertEquals("INCIDENT", appeal.getRelatedEntityType());
    }
}

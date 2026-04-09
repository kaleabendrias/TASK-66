package com.demo.app.application.service;

import com.demo.app.DemoApplication;
import com.demo.app.TestFixtures;
import com.demo.app.domain.enums.Role;
import com.demo.app.domain.model.Appeal;
import com.demo.app.persistence.entity.AppealEntity;
import com.demo.app.persistence.entity.UserEntity;
import com.demo.app.persistence.repository.AppealRepository;
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

    private UserEntity user;
    private UserEntity reviewer;

    @BeforeEach
    void setUp() {
        user = userRepository.save(TestFixtures.user("appealuser", Role.MEMBER));
        reviewer = userRepository.save(TestFixtures.user("appealreviewer", Role.MODERATOR));
    }

    @Test
    @DisplayName("create sets SUBMITTED status")
    void testCreate_setsSubmittedStatus() {
        Appeal appeal = appealService.create(user.getId(), "ORDER", 1L, "Unfair cancellation");

        assertNotNull(appeal.getId());
        assertEquals("SUBMITTED", appeal.getStatus());
        assertEquals(user.getId(), appeal.getUserId());
    }

    @Test
    @DisplayName("review as APPROVED sets resolvedAt")
    void testReview_approve_setsResolvedAt() {
        Appeal appeal = appealService.create(user.getId(), "ORDER", 1L, "Reason");

        Appeal reviewed = appealService.review(appeal.getId(), reviewer.getId(), "APPROVED", "Looks valid");

        assertEquals("APPROVED", reviewed.getStatus());
        assertNotNull(reviewed.getResolvedAt());
    }

    @Test
    @DisplayName("review as REJECTED sets resolvedAt")
    void testReview_reject_setsResolvedAt() {
        Appeal appeal = appealService.create(user.getId(), "ORDER", 1L, "Reason");

        Appeal reviewed = appealService.review(appeal.getId(), reviewer.getId(), "REJECTED", "Not valid");

        assertEquals("REJECTED", reviewed.getStatus());
        assertNotNull(reviewed.getResolvedAt());
    }

    @Test
    @DisplayName("review on already resolved appeal throws")
    void testReview_alreadyResolved_throws() {
        Appeal appeal = appealService.create(user.getId(), "ORDER", 1L, "Reason");
        appealService.review(appeal.getId(), reviewer.getId(), "APPROVED", "OK");

        assertThrows(IllegalArgumentException.class,
                () -> appealService.review(appeal.getId(), reviewer.getId(), "REJECTED", "Changed mind"));
    }

    @Test
    @DisplayName("review from UNDER_REVIEW status is allowed")
    void testReview_underReview_allowsReview() {
        Appeal appeal = appealService.create(user.getId(), "ORDER", 1L, "Reason");

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
        appealService.create(user.getId(), "ORDER", 1L, "Pending 1");
        appealService.create(user.getId(), "ORDER", 2L, "Pending 2");

        Appeal toApprove = appealService.create(user.getId(), "ORDER", 3L, "Will be approved");
        appealService.review(toApprove.getId(), reviewer.getId(), "APPROVED", "OK");

        List<Appeal> pending = appealService.getPending();

        assertEquals(2, pending.size());
        assertTrue(pending.stream().allMatch(a -> "SUBMITTED".equals(a.getStatus())));
    }

    @Test
    @DisplayName("Transition from SUBMITTED to APPROVED is valid")
    void testTransition_submitted_to_approved_valid() {
        Appeal appeal = appealService.create(user.getId(), "ORDER", 1L, "Reason");

        Appeal reviewed = appealService.review(appeal.getId(), reviewer.getId(), "APPROVED", "Valid");

        assertEquals("APPROVED", reviewed.getStatus());
    }

    @Test
    @DisplayName("Transition from SUBMITTED to REJECTED is valid")
    void testTransition_submitted_to_rejected_valid() {
        Appeal appeal = appealService.create(user.getId(), "ORDER", 1L, "Reason");

        Appeal reviewed = appealService.review(appeal.getId(), reviewer.getId(), "REJECTED", "Invalid");

        assertEquals("REJECTED", reviewed.getStatus());
    }
}

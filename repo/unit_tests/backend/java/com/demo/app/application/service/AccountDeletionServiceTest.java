package com.demo.app.application.service;

import com.demo.app.DemoApplication;
import com.demo.app.TestFixtures;
import com.demo.app.domain.enums.Role;
import com.demo.app.persistence.entity.AccountDeletionRequestEntity;
import com.demo.app.persistence.entity.UserEntity;
import com.demo.app.persistence.repository.AccountDeletionRequestRepository;
import com.demo.app.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = DemoApplication.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("AccountDeletionService - Account deletion request lifecycle")
class AccountDeletionServiceTest {

    @Autowired
    private AccountDeletionService accountDeletionService;

    @Autowired
    private AccountDeletionRequestRepository accountDeletionRequestRepository;

    @Autowired
    private UserRepository userRepository;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(TestFixtures.user("deleteuser", Role.MEMBER));
    }

    @Test
    @DisplayName("requestDeletion creates a PENDING request")
    void testRequestDeletion_createsPendingRequest() {
        AccountDeletionRequestEntity request = accountDeletionService.requestDeletion(user.getId());

        assertNotNull(request.getId());
        assertEquals("PENDING", request.getStatus());
        assertEquals(user.getId(), request.getUserId());
    }

    @Test
    @DisplayName("requestDeletion sets cooling off period to ~30 days from now")
    void testRequestDeletion_setsCoolingOffPeriod() {
        LocalDateTime before = LocalDateTime.now();

        AccountDeletionRequestEntity request = accountDeletionService.requestDeletion(user.getId());

        assertNotNull(request.getCoolingOffEndsAt());
        LocalDateTime expectedMin = before.plusDays(29);
        LocalDateTime expectedMax = LocalDateTime.now().plusDays(31);
        assertTrue(request.getCoolingOffEndsAt().isAfter(expectedMin));
        assertTrue(request.getCoolingOffEndsAt().isBefore(expectedMax));
    }

    @Test
    @DisplayName("requestDeletion throws when a pending request already exists")
    void testRequestDeletion_duplicatePending_throws() {
        accountDeletionService.requestDeletion(user.getId());

        assertThrows(RuntimeException.class,
                () -> accountDeletionService.requestDeletion(user.getId()));
    }

    @Test
    @DisplayName("cancelDeletion sets CANCELLED status")
    void testCancelDeletion_setsCancelledStatus() {
        AccountDeletionRequestEntity request = accountDeletionService.requestDeletion(user.getId());

        AccountDeletionRequestEntity cancelled = accountDeletionService.cancelDeletion(request.getId(), user.getId());

        assertEquals("CANCELLED", cancelled.getStatus());
        assertNotNull(cancelled.getCancelledAt());
    }

    @Test
    @DisplayName("cancelDeletion by wrong user throws RuntimeException")
    void testCancelDeletion_wrongUser_throws() {
        AccountDeletionRequestEntity request = accountDeletionService.requestDeletion(user.getId());

        UserEntity otherUser = userRepository.save(TestFixtures.user("otheruser", Role.MEMBER));

        assertThrows(RuntimeException.class,
                () -> accountDeletionService.cancelDeletion(request.getId(), otherUser.getId()));
    }

    @Test
    @DisplayName("cancelDeletion on already processed request throws RuntimeException")
    void testCancelDeletion_alreadyProcessed_throws() {
        AccountDeletionRequestEntity request = accountDeletionService.requestDeletion(user.getId());

        // Manually set to PROCESSED
        request.setStatus("PROCESSED");
        request.setProcessedAt(LocalDateTime.now());
        accountDeletionRequestRepository.save(request);

        assertThrows(RuntimeException.class,
                () -> accountDeletionService.cancelDeletion(request.getId(), user.getId()));
    }

    @Test
    @DisplayName("processExpired disables the user when cooling off period has passed")
    void testProcessExpired_disablesUser() {
        AccountDeletionRequestEntity request = accountDeletionService.requestDeletion(user.getId());

        // Manually set coolingOffEndsAt to the past
        request.setCoolingOffEndsAt(LocalDateTime.now().minusDays(1));
        accountDeletionRequestRepository.save(request);

        accountDeletionService.processExpired();

        UserEntity updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertFalse(updatedUser.isEnabled());

        AccountDeletionRequestEntity updatedRequest = accountDeletionRequestRepository.findById(request.getId()).orElseThrow();
        assertEquals("PROCESSED", updatedRequest.getStatus());
    }

    @Test
    @DisplayName("processExpired skips non-PENDING requests even with past cooling off date")
    void testProcessExpired_pendingOnly_skipsOthers() {
        AccountDeletionRequestEntity request = accountDeletionService.requestDeletion(user.getId());

        // Cancel it, then set coolingOffEndsAt to the past
        accountDeletionService.cancelDeletion(request.getId(), user.getId());
        request = accountDeletionRequestRepository.findById(request.getId()).orElseThrow();
        request.setCoolingOffEndsAt(LocalDateTime.now().minusDays(1));
        accountDeletionRequestRepository.save(request);

        accountDeletionService.processExpired();

        UserEntity updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertTrue(updatedUser.isEnabled(), "User should still be enabled because request was CANCELLED");
    }
}

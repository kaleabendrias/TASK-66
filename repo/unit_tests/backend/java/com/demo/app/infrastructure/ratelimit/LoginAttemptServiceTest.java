package com.demo.app.infrastructure.ratelimit;

import com.demo.app.DemoApplication;
import com.demo.app.persistence.entity.LoginAttemptEntity;
import com.demo.app.persistence.repository.LoginAttemptRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = DemoApplication.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("LoginAttemptService - Login lockout logic")
class LoginAttemptServiceTest {

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private LoginAttemptRepository loginAttemptRepository;

    @Test
    @DisplayName("recordAttempt persists a login attempt entry")
    void testRecordAttempt_savesEntry() {
        loginAttemptService.recordAttempt("testuser", "192.168.1.1", false);

        List<LoginAttemptEntity> attempts = loginAttemptRepository.findAll();
        assertFalse(attempts.isEmpty());

        LoginAttemptEntity saved = attempts.get(0);
        assertEquals("testuser", saved.getUsername());
        assertEquals("192.168.1.1", saved.getIpAddress());
        assertFalse(saved.isSuccess());
        assertNotNull(saved.getAttemptedAt());
    }

    @Test
    @DisplayName("isLockedOut returns false when below the failure threshold")
    void testIsLockedOut_belowThreshold_returnsFalse() {
        for (int i = 0; i < 9; i++) {
            loginAttemptService.recordAttempt("user9", "10.0.0.1", false);
        }

        assertFalse(loginAttemptService.isLockedOut("user9"));
    }

    @Test
    @DisplayName("isLockedOut returns true when at the failure threshold within one hour")
    void testIsLockedOut_atThreshold_returnsTrue() {
        for (int i = 0; i < 10; i++) {
            loginAttemptService.recordAttempt("user10", "10.0.0.1", false);
        }

        assertTrue(loginAttemptService.isLockedOut("user10"));
    }

    @Test
    @DisplayName("getRecentFailedCount returns correct count of recent failures")
    void testGetRecentFailedCount_returnsCorrectCount() {
        loginAttemptService.recordAttempt("countuser", "10.0.0.1", false);
        loginAttemptService.recordAttempt("countuser", "10.0.0.1", false);
        loginAttemptService.recordAttempt("countuser", "10.0.0.1", true); // success, should not count

        long count = loginAttemptService.getRecentFailedCount("countuser");
        assertEquals(2, count);
    }

    @Test
    @DisplayName("isLockedOut returns false for a user with no failed attempts")
    void testIsLockedOut_noAttempts_returnsFalse() {
        assertFalse(loginAttemptService.isLockedOut("nonexistent"));
    }
}

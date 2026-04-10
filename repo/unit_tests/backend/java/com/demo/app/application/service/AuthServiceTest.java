package com.demo.app.application.service;

import com.demo.app.DemoApplication;
import com.demo.app.TestFixtures;
import com.demo.app.domain.enums.Role;
import com.demo.app.domain.model.User;
import com.demo.app.persistence.entity.LoginAttemptEntity;
import com.demo.app.persistence.entity.UserEntity;
import com.demo.app.persistence.repository.LoginAttemptRepository;
import com.demo.app.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.AuthenticationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = DemoApplication.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthService - authentication and registration logic")
class AuthServiceTest {

    @Autowired private AuthService authService;
    @Autowired private UserRepository userRepository;
    @Autowired private LoginAttemptRepository loginAttemptRepository;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(TestFixtures.user("authsvc_user", Role.MEMBER));
    }

    @Test
    @DisplayName("login with valid credentials returns token")
    void login_validCredentials_returnsToken() {
        String token = authService.login("authsvc_user", "password123");
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    @DisplayName("login with wrong password throws AuthenticationException")
    void login_wrongPassword_throwsAuthException() {
        assertThrows(AuthenticationException.class,
                () -> authService.login("authsvc_user", "wrongpassword"));
    }

    @Test
    @DisplayName("login when locked out throws RuntimeException with locked message")
    void login_lockedOut_throwsWithLockedMessage() {
        // Record 10 failed attempts to trigger lockout
        for (int i = 0; i < 10; i++) {
            loginAttemptRepository.save(LoginAttemptEntity.builder()
                    .username("authsvc_user")
                    .ipAddress("127.0.0.1")
                    .success(false)
                    .attemptedAt(LocalDateTime.now())
                    .build());
        }
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.login("authsvc_user", "password123"));
        assertTrue(ex.getMessage().contains("locked"));
    }

    @Test
    @DisplayName("register creates user with MEMBER role")
    void register_createsUserWithMemberRole() {
        User newUser = authService.register("newreg", "newreg@test.local", "password123", "New Reg");
        assertNotNull(newUser);
        assertEquals("newreg", newUser.getUsername());
        assertEquals(Role.MEMBER, newUser.getRole());
    }
}

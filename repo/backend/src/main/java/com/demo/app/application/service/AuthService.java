package com.demo.app.application.service;

import com.demo.app.domain.enums.Role;
import com.demo.app.domain.model.User;
import com.demo.app.infrastructure.ratelimit.LoginAttemptService;
import com.demo.app.persistence.entity.UserEntity;
import com.demo.app.persistence.repository.UserRepository;
import com.demo.app.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final LoginAttemptService loginAttemptService;

    public String login(String username, String password) {
        if (loginAttemptService.isLockedOut(username)) {
            throw new RuntimeException("Account temporarily locked due to too many failed login attempts. Try again in 15 minutes.");
        }

        String ip = resolveIp();

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
        } catch (AuthenticationException e) {
            loginAttemptService.recordAttempt(username, ip, false);
            throw e;
        }

        loginAttemptService.recordAttempt(username, ip, true);
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return jwtService.generateToken(user.getUsername(), user.getRole().name());
    }

    private String resolveIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs != null ? attrs.getRequest().getRemoteAddr() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    @Transactional
    public User register(String username, String email, String password, String displayName) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already taken: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already in use: " + email);
        }

        UserEntity entity = UserEntity.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .displayName(displayName)
                .role(Role.MEMBER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return userRepository.save(entity).toModel();
    }
}

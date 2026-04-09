package com.demo.app.infrastructure.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SecretsValidator {

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @Value("${app.encryption.secret:}")
    private String encryptionSecret;

    @PostConstruct
    public void validate() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("FATAL: APP_JWT_SECRET is not set. Export it before starting the application.");
        }
        if (jwtSecret.length() < 32) {
            throw new IllegalStateException("FATAL: APP_JWT_SECRET must be at least 32 hex characters.");
        }
        if (encryptionSecret == null || encryptionSecret.isBlank()) {
            throw new IllegalStateException("FATAL: APP_ENCRYPTION_SECRET is not set. Export it before starting the application.");
        }
        if (encryptionSecret.length() < 32) {
            throw new IllegalStateException("FATAL: APP_ENCRYPTION_SECRET must be at least 32 hex characters.");
        }
    }
}

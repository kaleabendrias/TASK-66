package com.demo.app;

import com.demo.app.domain.enums.ProductStatus;
import com.demo.app.domain.enums.Role;
import com.demo.app.persistence.entity.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class TestFixtures {

    private TestFixtures() {}

    public static UserEntity user(String username, Role role) {
        return UserEntity.builder()
                .username(username)
                .email(username + "@test.local")
                .passwordHash("$2a$10$CDdcj12dr65C27ckLRMFQevdNud3wkqYzCcyk5iCsqCihFJDF1Ol2") // password123
                .displayName("Test " + username)
                .role(role)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static CategoryEntity category(String name) {
        return CategoryEntity.builder()
                .name(name)
                .description("Test " + name)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static ProductEntity product(String name, BigDecimal price, CategoryEntity cat, UserEntity seller) {
        return ProductEntity.builder()
                .name(name)
                .description("desc")
                .price(price)
                .stockQuantity(100)
                .category(cat)
                .seller(seller)
                .status(ProductStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static MemberTierEntity tier(String name, int rank, int min, Integer max) {
        return MemberTierEntity.builder()
                .name(name)
                .rank(rank)
                .minSpend(min)
                .maxSpend(max)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static MemberProfileEntity profile(Long userId, Long tierId, int totalSpend) {
        return MemberProfileEntity.builder()
                .userId(userId)
                .tierId(tierId)
                .totalSpend(totalSpend)
                .joinedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static LoginAttemptEntity loginAttempt(String username, String ip, boolean success) {
        return LoginAttemptEntity.builder()
                .username(username)
                .ipAddress(ip)
                .success(success)
                .attemptedAt(LocalDateTime.now())
                .build();
    }

    public static AuditLogEntity auditLog(String entityType, Long entityId, String action, Long actorId) {
        return AuditLogEntity.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .actorId(actorId)
                .ipAddress("127.0.0.1")
                .createdAt(LocalDateTime.now())
                .retentionExpiresAt(LocalDateTime.now().plusYears(2))
                .build();
    }
}

package com.demo.app.infrastructure.audit;

import com.demo.app.DemoApplication;
import com.demo.app.persistence.entity.AuditLogEntity;
import com.demo.app.persistence.repository.AuditLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = DemoApplication.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("AuditService - Audit logging and retention")
class AuditServiceTest {

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    @DisplayName("log() creates an audit entry in the repository")
    void testLog_createsAuditEntry() {
        auditService.log("Product", 1L, "CREATE", 100L,
                null, Map.of("name", "Widget"), "192.168.1.1");

        List<AuditLogEntity> entries = auditLogRepository.findAll();
        assertFalse(entries.isEmpty());

        AuditLogEntity entry = entries.get(0);
        assertEquals("Product", entry.getEntityType());
        assertEquals(1L, entry.getEntityId());
        assertEquals("CREATE", entry.getAction());
        assertEquals(100L, entry.getActorId());
        assertNull(entry.getOldValue());
        assertNotNull(entry.getNewValue());
        assertEquals("192.168.1.1", entry.getIpAddress());
    }

    @Test
    @DisplayName("log() sets retention expiration to approximately two years from now")
    void testLog_setsRetentionToTwoYears() {
        LocalDateTime beforeLog = LocalDateTime.now();

        auditService.log("Order", 42L, "UPDATE", 200L,
                Map.of("status", "PENDING"), Map.of("status", "SHIPPED"), "10.0.0.1");

        List<AuditLogEntity> entries = auditLogRepository.findAll();
        assertFalse(entries.isEmpty());

        AuditLogEntity entry = entries.get(0);
        assertNotNull(entry.getRetentionExpiresAt());

        LocalDateTime expectedMin = beforeLog.plusYears(2).minusMinutes(1);
        LocalDateTime expectedMax = LocalDateTime.now().plusYears(2).plusMinutes(1);

        assertTrue(entry.getRetentionExpiresAt().isAfter(expectedMin),
                "Retention date should be at least ~2 years from before the log call");
        assertTrue(entry.getRetentionExpiresAt().isBefore(expectedMax),
                "Retention date should be at most ~2 years from now");
    }

    @Test
    @DisplayName("getByEntity returns only matching entries")
    void testGetByEntity_returnsMatchingEntries() {
        auditService.log("User", 10L, "CREATE", 1L, null, null, "1.1.1.1");
        auditService.log("User", 10L, "UPDATE", 1L, null, null, "1.1.1.1");
        auditService.log("User", 20L, "CREATE", 1L, null, null, "1.1.1.1");
        auditService.log("Product", 10L, "CREATE", 1L, null, null, "1.1.1.1");

        List<AuditLogEntity> results = auditService.getByEntity("User", 10L);

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(e -> "User".equals(e.getEntityType()) && e.getEntityId() == 10L));
    }

    @Test
    @DisplayName("purgeExpired deletes entries with past retention dates")
    void testPurgeExpired_deletesOldEntries() {
        // Insert an entry with an already-expired retention date
        AuditLogEntity expired = AuditLogEntity.builder()
                .entityType("OldEntity")
                .entityId(999L)
                .action("DELETE")
                .actorId(1L)
                .ipAddress("127.0.0.1")
                .createdAt(LocalDateTime.now().minusYears(3))
                .retentionExpiresAt(LocalDateTime.now().minusDays(1))
                .build();
        auditLogRepository.save(expired);

        // Insert a current entry that should survive
        auditService.log("CurrentEntity", 1L, "CREATE", 1L, null, null, "127.0.0.1");

        auditService.purgeExpired();

        List<AuditLogEntity> remaining = auditLogRepository.findAll();
        assertEquals(1, remaining.size());
        assertEquals("CurrentEntity", remaining.get(0).getEntityType());
    }
}

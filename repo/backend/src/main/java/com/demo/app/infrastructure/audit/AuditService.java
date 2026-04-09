package com.demo.app.infrastructure.audit;

import com.demo.app.persistence.entity.AuditLogEntity;
import com.demo.app.persistence.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void log(String entityType, Long entityId, String action, Long actorId,
                    Object oldValue, Object newValue, String ipAddress) {
        try {
            String oldValueJson = oldValue != null ? objectMapper.writeValueAsString(oldValue) : null;
            String newValueJson = newValue != null ? objectMapper.writeValueAsString(newValue) : null;

            AuditLogEntity entity = AuditLogEntity.builder()
                    .entityType(entityType)
                    .entityId(entityId)
                    .action(action)
                    .actorId(actorId)
                    .oldValue(oldValueJson)
                    .newValue(newValueJson)
                    .ipAddress(ipAddress)
                    .createdAt(LocalDateTime.now())
                    .retentionExpiresAt(LocalDateTime.now().plusYears(2))
                    .build();

            auditLogRepository.save(entity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create audit log", e);
        }
    }

    public List<AuditLogEntity> getByEntity(String entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    @Transactional
    public void purgeExpired() {
        auditLogRepository.deleteExpired(LocalDateTime.now());
    }
}

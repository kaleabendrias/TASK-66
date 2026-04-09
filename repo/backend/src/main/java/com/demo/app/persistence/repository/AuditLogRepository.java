package com.demo.app.persistence.repository;

import com.demo.app.persistence.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

    List<AuditLogEntity> findByEntityTypeAndEntityId(String entityType, Long entityId);

    @Modifying
    @Query("DELETE FROM AuditLogEntity a WHERE a.retentionExpiresAt < :now")
    void deleteExpired(@Param("now") LocalDateTime now);
}

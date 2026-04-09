package com.demo.app.persistence.repository;

import com.demo.app.persistence.entity.RiskEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface RiskEventRepository extends JpaRepository<RiskEventEntity, Long> {

    List<RiskEventEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<RiskEventEntity> findByUserIdAndCreatedAtAfter(Long userId, LocalDateTime since);

    long countByUserIdAndEventType(Long userId, String eventType);
}

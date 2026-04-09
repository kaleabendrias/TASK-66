package com.demo.app.persistence.repository;

import com.demo.app.persistence.entity.AppealEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppealRepository extends JpaRepository<AppealEntity, Long> {

    List<AppealEntity> findByUserId(Long userId);

    List<AppealEntity> findByStatus(String status);

    List<AppealEntity> findByRelatedEntityTypeAndRelatedEntityId(String relatedEntityType, Long relatedEntityId);
}

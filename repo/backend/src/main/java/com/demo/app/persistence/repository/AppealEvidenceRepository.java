package com.demo.app.persistence.repository;

import com.demo.app.persistence.entity.AppealEvidenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppealEvidenceRepository extends JpaRepository<AppealEvidenceEntity, Long> {
    List<AppealEvidenceEntity> findByAppealId(Long appealId);
    long countByAppealId(Long appealId);
}

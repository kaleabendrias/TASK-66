package com.demo.app.persistence.repository;

import com.demo.app.persistence.entity.IncidentCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentCommentRepository extends JpaRepository<IncidentCommentEntity, Long> {

    List<IncidentCommentEntity> findByIncidentIdOrderByCreatedAtAsc(Long incidentId);
}

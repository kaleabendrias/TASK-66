package com.demo.app.persistence.repository;

import com.demo.app.persistence.entity.IncidentEscalationLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentEscalationLogRepository extends JpaRepository<IncidentEscalationLogEntity, Long> {

    List<IncidentEscalationLogEntity> findByIncidentId(Long incidentId);
}

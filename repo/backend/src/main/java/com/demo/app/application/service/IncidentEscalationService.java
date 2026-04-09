package com.demo.app.application.service;

import com.demo.app.domain.enums.Role;
import com.demo.app.persistence.entity.IncidentEntity;
import com.demo.app.persistence.entity.IncidentEscalationLogEntity;
import com.demo.app.persistence.entity.UserEntity;
import com.demo.app.persistence.repository.IncidentEscalationLogRepository;
import com.demo.app.persistence.repository.IncidentRepository;
import com.demo.app.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class IncidentEscalationService {

    private static final Logger log = LoggerFactory.getLogger(IncidentEscalationService.class);

    private final IncidentRepository incidentRepository;
    private final IncidentEscalationLogRepository incidentEscalationLogRepository;
    private final UserRepository userRepository;

    public void checkAndEscalate() {
        LocalDateTime now = LocalDateTime.now();

        List<IncidentEntity> unacknowledged = incidentRepository.findUnacknowledgedPastSla(now);
        for (IncidentEntity incident : unacknowledged) {
            escalate(incident, 1, "Unacknowledged past 15-minute SLA");

            if (incident.getAssigneeId() == null) {
                Optional<UserEntity> moderator = userRepository.findFirstByRole(Role.MODERATOR);
                moderator.ifPresent(mod -> {
                    incident.setAssigneeId(mod.getId());
                    log.warn("Auto-assigned incident {} to moderator {} ({})",
                            incident.getId(), mod.getId(), mod.getUsername());
                });
            }

            log.warn("Escalated incident {} to level 1: unacknowledged past SLA", incident.getId());
            incidentRepository.save(incident);
        }

        List<IncidentEntity> unresolved = incidentRepository.findUnresolvedPastSla(now);
        for (IncidentEntity incident : unresolved) {
            if (incident.getEscalationLevel() < 2) {
                escalate(incident, 2, "Unresolved past 24-hour SLA");
                log.warn("Escalated incident {} to level 2: unresolved past SLA", incident.getId());
                incidentRepository.save(incident);
            }
        }
    }

    private void escalate(IncidentEntity incident, int toLevel, String reason) {
        IncidentEscalationLogEntity logEntry = IncidentEscalationLogEntity.builder()
                .incidentId(incident.getId())
                .fromLevel(incident.getEscalationLevel())
                .toLevel(toLevel)
                .reason(reason)
                .createdAt(LocalDateTime.now())
                .build();

        incidentEscalationLogRepository.save(logEntry);
        incident.setEscalationLevel(toLevel);
        incident.setUpdatedAt(LocalDateTime.now());
    }
}

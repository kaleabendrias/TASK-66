package com.demo.app.application.service;

import com.demo.app.domain.enums.Role;
import com.demo.app.domain.exception.ResourceNotFoundException;
import com.demo.app.domain.model.Incident;
import com.demo.app.domain.model.IncidentComment;
import com.demo.app.persistence.entity.IncidentCommentEntity;
import com.demo.app.persistence.entity.IncidentEntity;
import com.demo.app.persistence.entity.UserEntity;
import com.demo.app.persistence.repository.IncidentCommentRepository;
import com.demo.app.persistence.repository.IncidentRepository;
import com.demo.app.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.demo.app.infrastructure.config.StatusTransitions;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentCommentRepository incidentCommentRepository;
    private final UserRepository userRepository;

    @Value("${app.incident.ack-sla-minutes:15}")
    private int ackSlaMinutes;

    @Value("${app.incident.resolve-sla-hours:24}")
    private int resolveSlaHours;

    private static final List<String> STATUS_ORDER = List.of(
            "OPEN", "ACKNOWLEDGED", "IN_PROGRESS", "RESOLVED", "CLOSED"
    );

    public Incident create(Long reporterId, String incidentType, String severity, String title, String description,
                           String address, String crossStreet, Long sellerId) {
        LocalDateTime now = LocalDateTime.now();

        int ackMinutes = ackSlaMinutes;
        int resolveHours = resolveSlaHours;

        if ("EMERGENCY".equals(severity)) {
            ackMinutes = ackMinutes / 2;
            resolveHours = resolveHours / 2;
        }

        if (sellerId != null) {
            // Risk analytics aggregates incidents by seller. If we let any
            // user id slip in here, the score for non-sellers becomes
            // meaningless and the seller-scoped windows are polluted.
            UserEntity sellerUser = userRepository.findById(sellerId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "sellerId does not reference an existing user: " + sellerId));
            if (sellerUser.getRole() != Role.SELLER) {
                throw new IllegalArgumentException(
                        "sellerId must reference a user with role SELLER, got " + sellerUser.getRole());
            }
        }

        IncidentEntity entity = IncidentEntity.builder()
                .reporterId(reporterId)
                .sellerId(sellerId)
                .incidentType(incidentType)
                .severity(severity)
                .title(title)
                .description(description)
                .address(address)
                .crossStreet(crossStreet)
                .status("OPEN")
                .escalationLevel(0)
                .slaAckDeadline(now.plusMinutes(ackMinutes))
                .slaResolveDeadline(now.plusHours(resolveHours))
                .createdAt(now)
                .updatedAt(now)
                .build();

        return incidentRepository.save(entity).toModel();
    }

    public Incident acknowledge(Long incidentId, Long assigneeId) {
        IncidentEntity entity = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", incidentId));

        int currentIndex = STATUS_ORDER.indexOf(entity.getStatus());
        int ackIndex = STATUS_ORDER.indexOf("ACKNOWLEDGED");
        if (currentIndex >= ackIndex) {
            return entity.toModel();
        }

        LocalDateTime now = LocalDateTime.now();
        entity.setStatus("ACKNOWLEDGED");
        entity.setAssigneeId(assigneeId);
        entity.setAcknowledgedAt(now);
        entity.setUpdatedAt(now);

        return incidentRepository.save(entity).toModel();
    }

    public Incident updateStatus(Long incidentId, String newStatus, String closureCode) {
        IncidentEntity entity = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", incidentId));

        StatusTransitions.validate(StatusTransitions.INCIDENT, entity.getStatus(), newStatus);

        if ("RESOLVED".equals(newStatus) && (closureCode == null || closureCode.isBlank())) {
            throw new IllegalArgumentException("Closure code is required when resolving an incident");
        }

        LocalDateTime now = LocalDateTime.now();
        entity.setStatus(newStatus);
        entity.setUpdatedAt(now);

        if ("RESOLVED".equals(newStatus)) {
            entity.setResolvedAt(now);
            entity.setClosureCode(closureCode);
        }

        return incidentRepository.save(entity).toModel();
    }

    public IncidentComment addComment(Long incidentId, Long authorId, String content) {
        IncidentCommentEntity entity = IncidentCommentEntity.builder()
                .incidentId(incidentId)
                .authorId(authorId)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build();

        return incidentCommentRepository.save(entity).toModel();
    }

    @Transactional(readOnly = true)
    public Incident getById(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", id))
                .toModel();
    }

    @Transactional(readOnly = true)
    public List<Incident> getByReporter(Long reporterId) {
        return incidentRepository.findByReporterId(reporterId).stream()
                .map(IncidentEntity::toModel)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Incident> getAll() {
        return incidentRepository.findAll().stream()
                .map(IncidentEntity::toModel)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<IncidentComment> getComments(Long incidentId) {
        return incidentCommentRepository.findByIncidentIdOrderByCreatedAtAsc(incidentId).stream()
                .map(IncidentCommentEntity::toModel)
                .toList();
    }
}

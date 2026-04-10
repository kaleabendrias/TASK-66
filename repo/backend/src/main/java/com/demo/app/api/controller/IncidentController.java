package com.demo.app.api.controller;

import com.demo.app.infrastructure.audit.Audited;
import com.demo.app.api.dto.*;
import com.demo.app.application.service.IncidentService;
import com.demo.app.domain.exception.OwnershipViolationException;
import com.demo.app.domain.model.Incident;
import com.demo.app.domain.model.IncidentComment;
import com.demo.app.persistence.entity.IncidentEscalationLogEntity;
import com.demo.app.persistence.repository.IncidentEscalationLogRepository;
import com.demo.app.persistence.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;
    private final IncidentEscalationLogRepository incidentEscalationLogRepository;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMINISTRATOR')")
    public ResponseEntity<List<IncidentDto>> getAll() {
        List<IncidentDto> incidents = incidentService.getAll().stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(incidents);
    }

    @GetMapping("/my")
    public ResponseEntity<List<IncidentDto>> getMy() {
        Long userId = getCurrentUserId();
        List<IncidentDto> incidents = incidentService.getByReporter(userId).stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(incidents);
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentDto> getById(@PathVariable Long id) {
        enforceIncidentAccess(id);
        return ResponseEntity.ok(toDto(incidentService.getById(id)));
    }

    @PostMapping
    @Audited(entityType = "INCIDENT", action = "CREATE")
    public ResponseEntity<IncidentDto> create(@Valid @RequestBody CreateIncidentRequest request) {
        Long reporterId = getCurrentUserId();
        Incident incident = incidentService.create(
                reporterId,
                request.incidentType().name(),
                request.severity().name(),
                request.title(),
                request.description(),
                request.address(),
                request.crossStreet()
        );
        return ResponseEntity.ok(toDto(incident));
    }

    @PostMapping("/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMINISTRATOR')")
    @Audited(entityType = "INCIDENT", action = "ACKNOWLEDGE")
    public ResponseEntity<IncidentDto> acknowledge(@PathVariable Long id) {
        Long assigneeId = getCurrentUserId();
        return ResponseEntity.ok(toDto(incidentService.acknowledge(id, assigneeId)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMINISTRATOR')")
    @Audited(entityType = "INCIDENT", action = "STATUS_CHANGE")
    public ResponseEntity<IncidentDto> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String newStatus = body.get("status");
        String closureCode = body.get("closureCode");
        return ResponseEntity.ok(toDto(incidentService.updateStatus(id, newStatus, closureCode)));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<IncidentCommentDto> addComment(@PathVariable Long id, @Valid @RequestBody AddCommentRequest request) {
        enforceIncidentAccess(id);
        Long authorId = getCurrentUserId();
        IncidentComment comment = incidentService.addComment(id, authorId, request.content());
        return ResponseEntity.ok(toCommentDto(comment));
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<IncidentCommentDto>> getComments(@PathVariable Long id) {
        enforceIncidentAccess(id);
        List<IncidentCommentDto> comments = incidentService.getComments(id).stream()
                .map(this::toCommentDto)
                .toList();
        return ResponseEntity.ok(comments);
    }

    @GetMapping("/{id}/escalations")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMINISTRATOR')")
    public ResponseEntity<List<IncidentEscalationLogEntity>> getEscalations(@PathVariable Long id) {
        return ResponseEntity.ok(incidentEscalationLogRepository.findByIncidentId(id));
    }

    private boolean isPrivileged() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMINISTRATOR") ||
                a.getAuthority().equals("ROLE_MODERATOR"));
    }

    private void enforceIncidentAccess(Long incidentId) {
        if (isPrivileged()) return;
        Incident incident = incidentService.getById(incidentId);
        Long currentUserId = getCurrentUserId();
        if (!incident.getReporterId().equals(currentUserId)) {
            throw new OwnershipViolationException("You do not have access to this incident");
        }
    }

    private Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username))
                .getId();
    }

    private IncidentDto toDto(Incident i) {
        return new IncidentDto(
                i.getId(),
                i.getReporterId(),
                i.getAssigneeId(),
                i.getIncidentType(),
                i.getSeverity(),
                i.getTitle(),
                i.getDescription(),
                i.getStatus(),
                i.getSlaAckDeadline(),
                i.getSlaResolveDeadline(),
                i.getEscalationLevel(),
                i.getCreatedAt(),
                i.getAcknowledgedAt(),
                i.getResolvedAt(),
                i.getAddress(),
                i.getCrossStreet(),
                i.getClosureCode()
        );
    }

    private IncidentCommentDto toCommentDto(IncidentComment c) {
        return new IncidentCommentDto(
                c.getId(),
                c.getIncidentId(),
                c.getAuthorId(),
                c.getContent(),
                c.getCreatedAt()
        );
    }
}

package com.demo.app.persistence.entity;

import com.demo.app.domain.model.Incident;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "incident")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Column(name = "assignee_id")
    private Long assigneeId;

    // The seller the incident is filed *against* (owner of the listing, product,
    // or account that the complaint concerns). Independent from reporterId
    // (who filed) and assigneeId (moderator handling triage).
    @Column(name = "seller_id")
    private Long sellerId;

    @Column(name = "incident_type", nullable = false)
    private String incidentType;

    @Column(name = "severity", nullable = false)
    private String severity;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "sla_ack_deadline")
    private LocalDateTime slaAckDeadline;

    @Column(name = "sla_resolve_deadline")
    private LocalDateTime slaResolveDeadline;

    @Column(name = "escalation_level", nullable = false)
    private int escalationLevel;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "cross_street", length = 255)
    private String crossStreet;

    @Column(name = "closure_code", length = 50)
    private String closureCode;

    public Incident toModel() {
        return Incident.builder()
                .id(id)
                .reporterId(reporterId)
                .assigneeId(assigneeId)
                .sellerId(sellerId)
                .incidentType(incidentType)
                .severity(severity)
                .title(title)
                .description(description)
                .status(status)
                .slaAckDeadline(slaAckDeadline)
                .slaResolveDeadline(slaResolveDeadline)
                .escalationLevel(escalationLevel)
                .createdAt(createdAt)
                .acknowledgedAt(acknowledgedAt)
                .resolvedAt(resolvedAt)
                .updatedAt(updatedAt)
                .address(address)
                .crossStreet(crossStreet)
                .closureCode(closureCode)
                .build();
    }
}

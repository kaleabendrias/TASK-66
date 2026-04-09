package com.demo.app.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "incident_escalation_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentEscalationLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "incident_id", nullable = false)
    private Long incidentId;

    @Column(name = "from_level", nullable = false)
    private int fromLevel;

    @Column(name = "to_level", nullable = false)
    private int toLevel;

    @Column(name = "reason")
    private String reason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}

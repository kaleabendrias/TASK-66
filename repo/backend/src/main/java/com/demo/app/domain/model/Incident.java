package com.demo.app.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Incident {
    private Long id;
    private Long reporterId;
    private Long assigneeId;
    private String incidentType;
    private String severity;
    private String title;
    private String description;
    private String status;
    private LocalDateTime slaAckDeadline;
    private LocalDateTime slaResolveDeadline;
    private int escalationLevel;
    private LocalDateTime createdAt;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime updatedAt;
    private String address;
    private String crossStreet;
}

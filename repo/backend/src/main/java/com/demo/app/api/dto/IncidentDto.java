package com.demo.app.api.dto;

import java.time.LocalDateTime;

public record IncidentDto(
        Long id,
        Long reporterId,
        Long assigneeId,
        Long sellerId,
        String incidentType,
        String severity,
        String title,
        String description,
        String status,
        LocalDateTime slaAckDeadline,
        LocalDateTime slaResolveDeadline,
        int escalationLevel,
        LocalDateTime createdAt,
        LocalDateTime acknowledgedAt,
        LocalDateTime resolvedAt,
        String address,
        String crossStreet,
        String closureCode
) {}

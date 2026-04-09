package com.demo.app.api.dto;

import java.time.LocalDateTime;

public record AppealDto(
        Long id,
        Long userId,
        String relatedEntityType,
        Long relatedEntityId,
        String reason,
        String status,
        Long reviewerId,
        String reviewNotes,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt,
        LocalDateTime resolvedAt
) {}

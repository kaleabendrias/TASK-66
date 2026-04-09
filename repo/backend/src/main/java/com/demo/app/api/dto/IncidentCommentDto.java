package com.demo.app.api.dto;

import java.time.LocalDateTime;

public record IncidentCommentDto(
        Long id,
        Long incidentId,
        Long authorId,
        String content,
        LocalDateTime createdAt
) {}

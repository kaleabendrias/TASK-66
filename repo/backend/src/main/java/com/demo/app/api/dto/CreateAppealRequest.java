package com.demo.app.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAppealRequest(
        String relatedEntityType,
        Long relatedEntityId,
        @NotBlank String reason
) {}

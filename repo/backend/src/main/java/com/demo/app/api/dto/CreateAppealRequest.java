package com.demo.app.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateAppealRequest(
        @NotBlank String relatedEntityType,
        @NotNull @Positive Long relatedEntityId,
        @NotBlank String reason
) {}

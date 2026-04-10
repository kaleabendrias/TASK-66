package com.demo.app.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record RecordRiskEventRequest(
        @NotNull Long userId,
        @NotBlank String eventType,
        @NotBlank String severity,
        Map<String, Object> details) {}

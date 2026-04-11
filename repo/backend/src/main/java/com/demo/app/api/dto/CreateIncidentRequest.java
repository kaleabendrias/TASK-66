package com.demo.app.api.dto;

import com.demo.app.domain.enums.IncidentSeverity;
import com.demo.app.domain.enums.IncidentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateIncidentRequest(
        @NotNull IncidentType incidentType,
        @NotNull IncidentSeverity severity,
        @NotBlank String title,
        @NotBlank String description,
        String address,
        String crossStreet,
        Long sellerId) {
}

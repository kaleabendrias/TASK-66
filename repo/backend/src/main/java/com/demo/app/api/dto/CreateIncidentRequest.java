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
        // Required: the risk analytics engine attributes incidents to a
        // seller's 30-day window. A null sellerId would silently degrade
        // seller-scoped scoring, so we reject the request at the boundary.
        @NotNull Long sellerId) {
}


package com.demo.app.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateIncidentRequest(
        String incidentType,
        String severity,
        String title,
        @NotBlank String description,
        String address,
        String crossStreet
) {}

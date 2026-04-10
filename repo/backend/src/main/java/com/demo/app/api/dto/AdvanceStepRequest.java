package com.demo.app.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AdvanceStepRequest(@NotBlank String stepName, String notes) {
}

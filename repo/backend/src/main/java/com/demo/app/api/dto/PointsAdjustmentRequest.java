package com.demo.app.api.dto;

import jakarta.validation.constraints.NotNull;

public record PointsAdjustmentRequest(@NotNull Integer amount, String reference) {
}

package com.demo.app.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReservationRequest(
        @NotNull Long inventoryItemId,
        @NotNull @Positive Integer quantity,
        @NotBlank String idempotencyKey) {
}

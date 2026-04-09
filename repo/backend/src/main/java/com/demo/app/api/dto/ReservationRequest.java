package com.demo.app.api.dto;

import jakarta.validation.constraints.NotNull;

public record ReservationRequest(@NotNull Long inventoryItemId, @NotNull Integer quantity, String idempotencyKey) {
}

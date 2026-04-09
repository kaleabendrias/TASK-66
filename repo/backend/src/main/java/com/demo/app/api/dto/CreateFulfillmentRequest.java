package com.demo.app.api.dto;

import jakarta.validation.constraints.NotNull;

public record CreateFulfillmentRequest(@NotNull Long orderId, @NotNull Long warehouseId, String idempotencyKey) {
}

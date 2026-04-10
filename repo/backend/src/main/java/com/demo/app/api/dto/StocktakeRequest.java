package com.demo.app.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record StocktakeRequest(
        @NotNull Long productId,
        @NotNull Long warehouseId,
        @NotNull @PositiveOrZero Integer countedQuantity,
        String referenceDocument) {}

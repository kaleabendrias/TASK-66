package com.demo.app.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record InboundRequest(
        @NotNull Long inventoryItemId,
        @NotNull @Positive Integer quantity,
        String referenceDocument,
        String notes) {}

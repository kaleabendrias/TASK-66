package com.demo.app.api.dto;

import java.time.LocalDateTime;

public record InventoryMovementDto(Long id, Long inventoryItemId, Long warehouseId, String movementType,
                                    int quantity, int balanceAfter, String referenceDocument,
                                    Long operatorId, String notes, LocalDateTime createdAt) {
}

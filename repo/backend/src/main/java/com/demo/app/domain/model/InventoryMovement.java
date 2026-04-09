package com.demo.app.domain.model;

import com.demo.app.domain.enums.MovementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryMovement {
    private Long id;
    private Long inventoryItemId;
    private Long warehouseId;
    private MovementType movementType;
    private int quantity;
    private int balanceAfter;
    private String referenceDocument;
    private Long operatorId;
    private String notes;
    private LocalDateTime createdAt;
}

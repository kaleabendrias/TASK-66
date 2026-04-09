package com.demo.app.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItem {
    private Long id;
    private Long productId;
    private Long warehouseId;
    private int quantityOnHand;
    private int quantityReserved;
    private int quantityAvailable;
    private int lowStockThreshold;
}

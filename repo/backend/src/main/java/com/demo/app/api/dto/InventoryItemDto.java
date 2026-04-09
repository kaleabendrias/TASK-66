package com.demo.app.api.dto;

public record InventoryItemDto(Long id, Long productId, Long warehouseId, String warehouseName,
                                int quantityOnHand, int quantityReserved, int quantityAvailable,
                                int lowStockThreshold, boolean lowStock) {
}

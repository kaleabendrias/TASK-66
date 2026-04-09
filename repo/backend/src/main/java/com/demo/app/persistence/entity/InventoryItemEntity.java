package com.demo.app.persistence.entity;

import com.demo.app.domain.model.InventoryItem;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_item")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "quantity_on_hand", nullable = false)
    private int quantityOnHand;

    @Column(name = "quantity_reserved", nullable = false)
    private int quantityReserved;

    @Column(name = "quantity_available", insertable = false, updatable = false)
    private int quantityAvailable;

    @Column(name = "low_stock_threshold", nullable = false)
    private int lowStockThreshold;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public InventoryItem toModel() {
        return InventoryItem.builder()
                .id(id)
                .productId(productId)
                .warehouseId(warehouseId)
                .quantityOnHand(quantityOnHand)
                .quantityReserved(quantityReserved)
                .quantityAvailable(quantityAvailable)
                .lowStockThreshold(lowStockThreshold)
                .build();
    }
}

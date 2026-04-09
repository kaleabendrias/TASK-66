package com.demo.app.persistence.entity;

import com.demo.app.domain.enums.MovementType;
import com.demo.app.domain.model.InventoryMovement;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_movement")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryMovementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inventory_item_id", nullable = false)
    private Long inventoryItemId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "movement_type", nullable = false)
    private String movementType;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "balance_after", nullable = false)
    private int balanceAfter;

    @Column(name = "reference_document")
    private String referenceDocument;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public InventoryMovement toModel() {
        return InventoryMovement.builder()
                .id(id)
                .inventoryItemId(inventoryItemId)
                .warehouseId(warehouseId)
                .movementType(MovementType.valueOf(movementType))
                .quantity(quantity)
                .balanceAfter(balanceAfter)
                .referenceDocument(referenceDocument)
                .operatorId(operatorId)
                .notes(notes)
                .createdAt(createdAt)
                .build();
    }
}

package com.demo.app.application.service;

import com.demo.app.domain.model.InventoryItem;
import com.demo.app.persistence.entity.InventoryItemEntity;
import com.demo.app.persistence.entity.InventoryMovementEntity;
import com.demo.app.persistence.repository.InventoryItemRepository;
import com.demo.app.persistence.repository.InventoryMovementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class InventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryMovementRepository inventoryMovementRepository;

    public InventoryItem getByProductAndWarehouse(Long productId, Long warehouseId) {
        return inventoryItemRepository.findByProductIdAndWarehouseId(productId, warehouseId)
                .orElseThrow(() -> new RuntimeException("Inventory item not found for product " + productId + " and warehouse " + warehouseId))
                .toModel();
    }

    public InventoryItem getById(Long inventoryItemId) {
        return inventoryItemRepository.findById(inventoryItemId)
                .orElseThrow(() -> new com.demo.app.domain.exception.ResourceNotFoundException(
                        "Inventory item", inventoryItemId))
                .toModel();
    }

    public List<InventoryItem> getByProduct(Long productId) {
        return inventoryItemRepository.findByProductId(productId).stream()
                .map(InventoryItemEntity::toModel)
                .toList();
    }

    public List<InventoryItem> getLowStockItems() {
        return inventoryItemRepository.findLowStock().stream()
                .map(InventoryItemEntity::toModel)
                .toList();
    }

    public InventoryItem adjustStock(Long inventoryItemId, int quantityChange, String movementType,
                                     String referenceDocument, Long operatorId, String notes) {
        InventoryItemEntity item = inventoryItemRepository.findById(inventoryItemId)
                .orElseThrow(() -> new RuntimeException("Inventory item not found: " + inventoryItemId));

        int newQuantityOnHand = item.getQuantityOnHand() + quantityChange;
        if (newQuantityOnHand < 0) {
            throw new RuntimeException("Insufficient stock. Current on-hand: " + item.getQuantityOnHand()
                    + ", requested change: " + quantityChange);
        }

        item.setQuantityOnHand(newQuantityOnHand);
        item.setUpdatedAt(LocalDateTime.now());
        inventoryItemRepository.save(item);

        InventoryMovementEntity movement = InventoryMovementEntity.builder()
                .inventoryItemId(inventoryItemId)
                .warehouseId(item.getWarehouseId())
                .movementType(movementType)
                .quantity(quantityChange)
                .balanceAfter(newQuantityOnHand)
                .referenceDocument(referenceDocument)
                .operatorId(operatorId)
                .notes(notes)
                .createdAt(LocalDateTime.now())
                .build();
        inventoryMovementRepository.save(movement);

        int available = newQuantityOnHand - item.getQuantityReserved();
        // Strict < 5 system-wide low-stock alert, or per-item threshold if higher
        int effectiveThreshold = Math.max(5, item.getLowStockThreshold());
        if (available < effectiveThreshold) {
            log.warn("LOW STOCK ALERT: item {} available={} below threshold={} (system minimum=5)",
                    inventoryItemId, available, effectiveThreshold);
        }

        return item.toModel();
    }
}

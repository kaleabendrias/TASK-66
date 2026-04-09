package com.demo.app.persistence.repository;

import com.demo.app.persistence.entity.InventoryMovementEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovementEntity, Long> {
    List<InventoryMovementEntity> findByInventoryItemIdOrderByCreatedAtDesc(Long inventoryItemId);
    List<InventoryMovementEntity> findByOperatorId(Long operatorId);
    List<InventoryMovementEntity> findByWarehouseId(Long warehouseId);
}

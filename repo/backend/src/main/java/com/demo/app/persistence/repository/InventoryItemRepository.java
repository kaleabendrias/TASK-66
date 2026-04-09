package com.demo.app.persistence.repository;

import com.demo.app.persistence.entity.InventoryItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InventoryItemRepository extends JpaRepository<InventoryItemEntity, Long> {
    List<InventoryItemEntity> findByProductId(Long productId);
    List<InventoryItemEntity> findByWarehouseId(Long warehouseId);
    Optional<InventoryItemEntity> findByProductIdAndWarehouseId(Long productId, Long warehouseId);

    @Query("SELECT i FROM InventoryItemEntity i WHERE (i.quantityOnHand - i.quantityReserved) < i.lowStockThreshold")
    List<InventoryItemEntity> findLowStock();
}

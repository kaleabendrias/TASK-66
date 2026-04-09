package com.demo.app.application.service;

import com.demo.app.DemoApplication;
import com.demo.app.TestFixtures;
import com.demo.app.domain.enums.Role;
import com.demo.app.domain.model.InventoryItem;
import com.demo.app.persistence.entity.*;
import com.demo.app.persistence.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = DemoApplication.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("InventoryService - Stock adjustments and low stock detection")
class InventoryServiceTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private InventoryMovementRepository inventoryMovementRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    private UserEntity user;
    private WarehouseEntity warehouse;
    private ProductEntity product;
    private InventoryItemEntity inventoryItem;

    @BeforeEach
    void setUp() {
        user = userRepository.save(TestFixtures.user("invuser", Role.WAREHOUSE_STAFF));
        CategoryEntity category = categoryRepository.save(TestFixtures.category("Parts"));
        product = productRepository.save(TestFixtures.product("Bolt", new BigDecimal("1.50"), category, user));

        warehouse = warehouseRepository.save(WarehouseEntity.builder()
                .name("Warehouse A")
                .code("WH-A")
                .location("Chicago")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build());

        inventoryItem = inventoryItemRepository.save(InventoryItemEntity.builder()
                .productId(product.getId())
                .warehouseId(warehouse.getId())
                .quantityOnHand(100)
                .quantityReserved(0)
                .lowStockThreshold(5)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
    }

    @Test
    @DisplayName("adjustStock with positive quantity increases on-hand quantity")
    void testAdjustStock_receipt_increasesQuantity() {
        InventoryItem result = inventoryService.adjustStock(
                inventoryItem.getId(), 50, "RECEIPT", "PO-001", user.getId(), "Received shipment");

        assertEquals(150, result.getQuantityOnHand());
    }

    @Test
    @DisplayName("adjustStock creates a movement record")
    void testAdjustStock_createsMovementRecord() {
        inventoryService.adjustStock(
                inventoryItem.getId(), 50, "RECEIPT", "PO-001", user.getId(), "Received shipment");

        List<InventoryMovementEntity> movements = inventoryMovementRepository
                .findByInventoryItemIdOrderByCreatedAtDesc(inventoryItem.getId());

        assertFalse(movements.isEmpty());
        InventoryMovementEntity movement = movements.get(0);
        assertEquals("RECEIPT", movement.getMovementType());
        assertEquals(50, movement.getQuantity());
        assertEquals(user.getId(), movement.getOperatorId());
    }

    @Test
    @DisplayName("adjustStock with negative quantity going below zero throws RuntimeException")
    void testAdjustStock_negativeGoingBelowZero_throws() {
        assertThrows(RuntimeException.class,
                () -> inventoryService.adjustStock(
                        inventoryItem.getId(), -150, "ADJUSTMENT", "ADJ-001", user.getId(), "Bad adjustment"));
    }

    @Test
    @DisplayName("getLowStockItems returns items below threshold")
    void testGetLowStockItems_belowThreshold_returned() {
        // Create a low-stock item
        CategoryEntity cat2 = categoryRepository.save(TestFixtures.category("LowCat"));
        ProductEntity prod2 = productRepository.save(TestFixtures.product("LowItem", new BigDecimal("5.00"), cat2, user));
        inventoryItemRepository.save(InventoryItemEntity.builder()
                .productId(prod2.getId())
                .warehouseId(warehouse.getId())
                .quantityOnHand(3)
                .quantityReserved(0)
                .lowStockThreshold(5)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        List<InventoryItem> lowStock = inventoryService.getLowStockItems();

        assertTrue(lowStock.stream().anyMatch(i -> i.getQuantityOnHand() == 3));
    }

    @Test
    @DisplayName("getLowStockItems does not return items above threshold")
    void testGetLowStockItems_aboveThreshold_notReturned() {
        // The default inventoryItem has 100 on hand, threshold 5, should NOT be in low stock
        List<InventoryItem> lowStock = inventoryService.getLowStockItems();

        assertTrue(lowStock.stream().noneMatch(i -> i.getId().equals(inventoryItem.getId())));
    }

    @Test
    @DisplayName("adjustStock movement has correct balanceAfter")
    void testAdjustStock_movementHasCorrectBalanceAfter() {
        inventoryService.adjustStock(
                inventoryItem.getId(), 50, "RECEIPT", "PO-002", user.getId(), null);

        List<InventoryMovementEntity> movements = inventoryMovementRepository
                .findByInventoryItemIdOrderByCreatedAtDesc(inventoryItem.getId());

        assertEquals(150, movements.get(0).getBalanceAfter());
    }

    @Test
    @DisplayName("Multiple adjustments maintain correct running balance")
    void testAdjustStock_multipleAdjustments_correctRunningBalance() {
        inventoryService.adjustStock(
                inventoryItem.getId(), 50, "RECEIPT", "PO-A", user.getId(), null);    // 100 + 50 = 150
        inventoryService.adjustStock(
                inventoryItem.getId(), -30, "ADJUSTMENT", "ADJ-A", user.getId(), null); // 150 - 30 = 120
        inventoryService.adjustStock(
                inventoryItem.getId(), 20, "RECEIPT", "PO-B", user.getId(), null);    // 120 + 20 = 140

        InventoryItemEntity updated = inventoryItemRepository.findById(inventoryItem.getId()).orElseThrow();
        assertEquals(140, updated.getQuantityOnHand());

        List<InventoryMovementEntity> movements = inventoryMovementRepository
                .findByInventoryItemIdOrderByCreatedAtDesc(inventoryItem.getId());

        assertEquals(3, movements.size());
        // Most recent movement should have balanceAfter = 140
        assertEquals(140, movements.get(0).getBalanceAfter());
    }
}

package com.demo.app.application.service;

import com.demo.app.DemoApplication;
import com.demo.app.TestFixtures;
import com.demo.app.domain.enums.Role;
import com.demo.app.domain.model.StockReservation;
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
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = DemoApplication.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("ReservationService - Stock reservation lifecycle")
class ReservationServiceTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private StockReservationRepository stockReservationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private InventoryMovementRepository inventoryMovementRepository;

    private UserEntity user;
    private InventoryItemEntity inventoryItem;

    @BeforeEach
    void setUp() {
        // Reservation expiry uses SystemOperatorProvider, which resolves the
        // first ADMINISTRATOR. Seed one so the lookup never returns empty.
        userRepository.save(TestFixtures.user("reserve_admin", Role.ADMINISTRATOR));
        user = userRepository.save(TestFixtures.user("reserveuser", Role.MEMBER));
        CategoryEntity category = categoryRepository.save(TestFixtures.category("Electronics"));
        ProductEntity product = productRepository.save(TestFixtures.product("Widget", new BigDecimal("29.99"), category, user));

        WarehouseEntity warehouse = warehouseRepository.save(WarehouseEntity.builder()
                .name("Main Warehouse")
                .code("WH-001")
                .location("New York")
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
    @DisplayName("reserve creates a HELD reservation")
    void testReserve_success_createsHeldReservation() {
        StockReservation reservation = reservationService.reserve(
                inventoryItem.getId(), user.getId(), 10, "key-1");

        assertNotNull(reservation.getId());
        assertEquals("HELD", reservation.getStatus().name());
        assertEquals(10, reservation.getQuantity());
    }

    @Test
    @DisplayName("reserve updates the reserved quantity on the inventory item")
    void testReserve_updatesReservedQuantity() {
        reservationService.reserve(inventoryItem.getId(), user.getId(), 10, "key-2");

        InventoryItemEntity updated = inventoryItemRepository.findById(inventoryItem.getId()).orElseThrow();
        assertEquals(10, updated.getQuantityReserved());
    }

    @Test
    @DisplayName("reserve with same idempotency key returns the same reservation")
    void testReserve_idempotent_returnsSameReservation() {
        StockReservation first = reservationService.reserve(
                inventoryItem.getId(), user.getId(), 10, "key-idem");
        StockReservation second = reservationService.reserve(
                inventoryItem.getId(), user.getId(), 10, "key-idem");

        assertEquals(first.getId(), second.getId());
    }

    @Test
    @DisplayName("reserve with insufficient stock throws RuntimeException")
    void testReserve_insufficientStock_throws() {
        // Set 95 already reserved, only 5 available
        inventoryItem.setQuantityReserved(95);
        inventoryItem.setUpdatedAt(LocalDateTime.now());
        inventoryItemRepository.save(inventoryItem);

        assertThrows(RuntimeException.class,
                () -> reservationService.reserve(inventoryItem.getId(), user.getId(), 10, "key-fail"));
    }

    @Test
    @DisplayName("confirm deducts stock from on-hand and reserved quantities")
    void testConfirm_success_deductsStock() {
        StockReservation reservation = reservationService.reserve(
                inventoryItem.getId(), user.getId(), 10, "key-confirm");

        reservationService.confirm(reservation.getId());

        InventoryItemEntity updated = inventoryItemRepository.findById(inventoryItem.getId()).orElseThrow();
        assertEquals(90, updated.getQuantityOnHand());
        assertEquals(0, updated.getQuantityReserved());
    }

    @Test
    @DisplayName("confirm on already confirmed reservation is idempotent")
    void testConfirm_alreadyConfirmed_idempotent() {
        StockReservation reservation = reservationService.reserve(
                inventoryItem.getId(), user.getId(), 10, "key-confirm2");

        StockReservation first = reservationService.confirm(reservation.getId());
        StockReservation second = reservationService.confirm(reservation.getId());

        assertEquals(first.getId(), second.getId());
        assertEquals("CONFIRMED", second.getStatus().name());
    }

    @Test
    @DisplayName("cancel releases reserved quantity back to inventory")
    void testCancel_success_releasesReservedQuantity() {
        StockReservation reservation = reservationService.reserve(
                inventoryItem.getId(), user.getId(), 10, "key-cancel");

        reservationService.cancel(reservation.getId());

        InventoryItemEntity updated = inventoryItemRepository.findById(inventoryItem.getId()).orElseThrow();
        assertEquals(0, updated.getQuantityReserved());
    }

    @Test
    @DisplayName("cancel on already cancelled reservation is idempotent")
    void testCancel_alreadyCancelled_idempotent() {
        StockReservation reservation = reservationService.reserve(
                inventoryItem.getId(), user.getId(), 10, "key-cancel2");

        StockReservation first = reservationService.cancel(reservation.getId());
        StockReservation second = reservationService.cancel(reservation.getId());

        assertEquals(first.getId(), second.getId());
        assertEquals("CANCELLED", second.getStatus().name());
    }

    @Test
    @DisplayName("expireOverdueReservations changes status to EXPIRED for overdue reservations")
    void testExpireOverdueReservations() {
        StockReservation reservation = reservationService.reserve(
                inventoryItem.getId(), user.getId(), 10, "key-expire");

        // Manually set expiresAt to the past
        StockReservationEntity entity = stockReservationRepository.findById(reservation.getId()).orElseThrow();
        entity.setExpiresAt(LocalDateTime.now().minusMinutes(10));
        stockReservationRepository.save(entity);

        reservationService.expireOverdueReservations();

        StockReservationEntity expired = stockReservationRepository.findById(reservation.getId()).orElseThrow();
        assertEquals("EXPIRED", expired.getStatus());
    }

    @Test
    @DisplayName("reserve sets expiresAt to approximately now + 30 minutes")
    void testReserve_30minuteExpiry() {
        LocalDateTime before = LocalDateTime.now();

        StockReservation reservation = reservationService.reserve(
                inventoryItem.getId(), user.getId(), 10, "key-expiry");

        LocalDateTime expectedMin = before.plusMinutes(29);
        LocalDateTime expectedMax = LocalDateTime.now().plusMinutes(31);

        assertTrue(reservation.getExpiresAt().isAfter(expectedMin),
                "expiresAt should be at least ~29 minutes from before the call");
        assertTrue(reservation.getExpiresAt().isBefore(expectedMax),
                "expiresAt should be at most ~31 minutes from now");
    }

    @Test
    @DisplayName("expireOverdueReservations persists a RESERVATION_RELEASE movement with a non-null operatorId")
    void testExpireOverdue_persistsMovementWithSystemOperator() {
        StockReservation reservation = reservationService.reserve(
                inventoryItem.getId(), user.getId(), 7, "key-persist-expiry");

        StockReservationEntity entity = stockReservationRepository.findById(reservation.getId()).orElseThrow();
        entity.setExpiresAt(LocalDateTime.now().minusMinutes(5));
        stockReservationRepository.save(entity);

        reservationService.expireOverdueReservations();

        // The expiry movement is the most recent for this inventory item.
        java.util.List<InventoryMovementEntity> movements = inventoryMovementRepository
                .findByInventoryItemIdOrderByCreatedAtDesc(inventoryItem.getId());
        assertFalse(movements.isEmpty());
        InventoryMovementEntity expiry = movements.stream()
                .filter(m -> m.getReferenceDocument() != null
                        && m.getReferenceDocument().startsWith("reservation-expired:"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("expiry movement not persisted"));

        // V5 schema requires operator_id NOT NULL — this assertion would fail
        // if we ever regress to writing the movement without one.
        assertNotNull(expiry.getOperatorId(), "operatorId is NOT NULL in V5 schema");
        assertEquals("RESERVATION_RELEASE", expiry.getMovementType());
        assertEquals(7, expiry.getQuantity());
        assertEquals(inventoryItem.getWarehouseId(), expiry.getWarehouseId());

        // The reserved counter must be released.
        InventoryItemEntity refreshed = inventoryItemRepository.findById(inventoryItem.getId()).orElseThrow();
        assertEquals(0, refreshed.getQuantityReserved());
    }
}

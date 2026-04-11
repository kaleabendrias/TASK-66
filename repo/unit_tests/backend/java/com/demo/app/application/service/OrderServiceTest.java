package com.demo.app.application.service;

import com.demo.app.DemoApplication;
import com.demo.app.TestFixtures;
import com.demo.app.domain.enums.OrderStatus;
import com.demo.app.domain.enums.Role;
import com.demo.app.domain.exception.ResourceNotFoundException;
import com.demo.app.domain.model.Order;
import com.demo.app.persistence.entity.CategoryEntity;
import com.demo.app.persistence.entity.InventoryItemEntity;
import com.demo.app.persistence.entity.OrderEntity;
import com.demo.app.persistence.entity.ProductEntity;
import com.demo.app.persistence.entity.StockReservationEntity;
import com.demo.app.persistence.entity.UserEntity;
import com.demo.app.persistence.entity.WarehouseEntity;
import com.demo.app.persistence.repository.CategoryRepository;
import com.demo.app.persistence.repository.InventoryItemRepository;
import com.demo.app.persistence.repository.OrderRepository;
import com.demo.app.persistence.repository.ProductRepository;
import com.demo.app.persistence.repository.StockReservationRepository;
import com.demo.app.persistence.repository.UserRepository;
import com.demo.app.persistence.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = DemoApplication.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("OrderService - order placement and status transitions")
class OrderServiceTest {

    @Autowired private OrderService orderService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private InventoryItemRepository inventoryItemRepository;
    @Autowired private WarehouseRepository warehouseRepository;
    @Autowired private StockReservationRepository stockReservationRepository;

    private UserEntity buyer;
    private ProductEntity product;
    private OrderEntity order;
    private InventoryItemEntity inventoryItem;

    @BeforeEach
    void setUp() {
        // SystemOperatorProvider needs at least one ADMINISTRATOR row.
        userRepository.save(TestFixtures.user("ordsvc_admin", Role.ADMINISTRATOR));
        buyer = userRepository.save(TestFixtures.user("ordsvc_buyer", Role.MEMBER));
        UserEntity seller = userRepository.save(TestFixtures.user("ordsvc_seller", Role.SELLER));
        CategoryEntity category = categoryRepository.save(TestFixtures.category("Books"));
        product = productRepository.save(TestFixtures.product("Novel", BigDecimal.valueOf(15), category, seller));

        WarehouseEntity warehouse = warehouseRepository.save(WarehouseEntity.builder()
                .name("Order test WH").code("OWH").location("Test").active(true)
                .createdAt(LocalDateTime.now()).build());
        inventoryItem = inventoryItemRepository.save(InventoryItemEntity.builder()
                .productId(product.getId()).warehouseId(warehouse.getId())
                .quantityOnHand(50).quantityReserved(0).lowStockThreshold(5)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        LocalDateTime now = LocalDateTime.now();
        order = orderRepository.save(OrderEntity.builder()
                .buyer(buyer).product(product)
                .quantity(1).totalPrice(BigDecimal.valueOf(15))
                .status(OrderStatus.PLACED).tenderType("INTERNAL_CREDIT")
                .createdAt(now).updatedAt(now).build());
    }

    @Test
    @DisplayName("getAll returns all orders")
    void getAll_returnsOrders() {
        var all = orderService.getAll();
        assertFalse(all.isEmpty());
        assertEquals(order.getId(), all.get(0).getId());
    }

    @Test
    @DisplayName("updateStatus valid transition succeeds")
    void updateStatus_validTransition_succeeds() {
        Order confirmed = orderService.updateStatus(order.getId(), OrderStatus.CONFIRMED);
        assertEquals(OrderStatus.CONFIRMED, confirmed.getStatus());
    }

    @Test
    @DisplayName("updateStatus invalid transition throws IllegalStateException")
    void updateStatus_invalidTransition_throws() {
        assertThrows(IllegalStateException.class,
                () -> orderService.updateStatus(order.getId(), OrderStatus.DELIVERED));
    }

    @Test
    @DisplayName("updateStatus idempotent - same status returns as-is")
    void updateStatus_idempotent_sameStatus() {
        Order same = orderService.updateStatus(order.getId(), OrderStatus.PLACED);
        assertEquals(OrderStatus.PLACED, same.getStatus());
    }

    @Test
    @DisplayName("getById returns order")
    void getById_returnsOrder() {
        Order found = orderService.getById(order.getId());
        assertEquals(order.getId(), found.getId());
    }

    @Test
    @DisplayName("getById non-existent throws ResourceNotFoundException")
    void getById_nonExistent_throws() {
        assertThrows(ResourceNotFoundException.class, () -> orderService.getById(99999L));
    }

    @Test
    @DisplayName("placeOrder ignores client-supplied total and persists the server-resolved price")
    void placeOrder_clientPriceTamperingIgnored() {
        // Product unit price is 15.00 from setUp; quantity 2 → server total should be 30.00.
        Order placed = orderService.placeOrder(
                buyer.getId(), product.getId(), 2,
                new BigDecimal("0.01"),  // tamper attempt
                null, inventoryItem.getId(), "tamper-test-1");

        assertEquals(0, new BigDecimal("30.00").compareTo(placed.getTotalPrice()),
                "server total must override the client tamper attempt");
        assertNotNull(placed.getReservationId(),
                "every placed order must be backed by a reservation");
    }

    @Test
    @DisplayName("placeOrder mints a HELD reservation that the order references")
    void placeOrder_createsHeldReservation() {
        Order placed = orderService.placeOrder(
                buyer.getId(), product.getId(), 3,
                new BigDecimal("45.00"),
                null, inventoryItem.getId(), "auto-hold-1");

        assertNotNull(placed.getReservationId());
        StockReservationEntity reservation = stockReservationRepository.findById(placed.getReservationId()).orElseThrow();
        assertEquals("HELD", reservation.getStatus());
        assertEquals(3, reservation.getQuantity());
        assertEquals(buyer.getId(), reservation.getUserId());
        assertEquals(inventoryItem.getId(), reservation.getInventoryItemId());

        InventoryItemEntity reloaded = inventoryItemRepository.findById(inventoryItem.getId()).orElseThrow();
        assertEquals(3, reloaded.getQuantityReserved(),
                "the placed order must have bumped the inventory item's reserved counter");
    }

    @Test
    @DisplayName("CANCELLED on a HELD order releases the reservation and the reserved counter")
    void placeOrder_cancelReleasesHeldReservation() {
        Order placed = orderService.placeOrder(
                buyer.getId(), product.getId(), 2,
                new BigDecimal("30.00"),
                null, inventoryItem.getId(), "cancel-held-1");

        orderService.updateStatus(placed.getId(), OrderStatus.CANCELLED, buyer.getId());

        StockReservationEntity reservation = stockReservationRepository.findById(placed.getReservationId()).orElseThrow();
        assertEquals("CANCELLED", reservation.getStatus());
        InventoryItemEntity reloaded = inventoryItemRepository.findById(inventoryItem.getId()).orElseThrow();
        assertEquals(0, reloaded.getQuantityReserved(),
                "cancelling a HELD order must release the reserved counter back to zero");
    }

    @Test
    @DisplayName("CANCELLED on a CONFIRMED order rolls back the on-hand quantity that was deducted at confirm")
    void placeOrder_cancelAfterConfirm_rollsBackOnHand() {
        Order placed = orderService.placeOrder(
                buyer.getId(), product.getId(), 4,
                new BigDecimal("60.00"),
                null, inventoryItem.getId(), "cancel-confirmed-1");

        int beforeOnHand = inventoryItemRepository.findById(inventoryItem.getId()).orElseThrow().getQuantityOnHand();

        orderService.updateStatus(placed.getId(), OrderStatus.CONFIRMED);
        InventoryItemEntity afterConfirm = inventoryItemRepository.findById(inventoryItem.getId()).orElseThrow();
        assertEquals(beforeOnHand - 4, afterConfirm.getQuantityOnHand(),
                "confirm should have deducted on-hand by the order quantity");

        orderService.updateStatus(placed.getId(), OrderStatus.CANCELLED, buyer.getId());

        InventoryItemEntity afterCancel = inventoryItemRepository.findById(inventoryItem.getId()).orElseThrow();
        assertEquals(beforeOnHand, afterCancel.getQuantityOnHand(),
                "rollbackConfirmed must restore the on-hand back to the pre-confirm value");
        StockReservationEntity reservation = stockReservationRepository.findById(placed.getReservationId()).orElseThrow();
        assertEquals("CANCELLED", reservation.getStatus());
    }

    @Test
    @DisplayName("FAILED status triggers the same compensation path as CANCELLED")
    void placeOrder_failedTriggersCompensation() {
        Order placed = orderService.placeOrder(
                buyer.getId(), product.getId(), 1,
                new BigDecimal("15.00"),
                null, inventoryItem.getId(), "failed-1");

        orderService.updateStatus(placed.getId(), OrderStatus.FAILED, buyer.getId());

        StockReservationEntity reservation = stockReservationRepository.findById(placed.getReservationId()).orElseThrow();
        assertEquals("CANCELLED", reservation.getStatus(),
                "FAILED must release the reservation just like CANCELLED");
    }
}

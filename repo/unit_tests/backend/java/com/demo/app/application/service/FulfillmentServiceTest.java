package com.demo.app.application.service;

import com.demo.app.DemoApplication;
import com.demo.app.TestFixtures;
import com.demo.app.domain.enums.FulfillmentStatus;
import com.demo.app.domain.enums.OrderStatus;
import com.demo.app.domain.enums.Role;
import com.demo.app.domain.model.Fulfillment;
import com.demo.app.domain.model.FulfillmentStep;
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
@DisplayName("FulfillmentService - Order fulfillment workflow")
class FulfillmentServiceTest {

    @Autowired
    private FulfillmentService fulfillmentService;

    @Autowired
    private FulfillmentRepository fulfillmentRepository;

    @Autowired
    private FulfillmentStepRepository fulfillmentStepRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private OrderRepository orderRepository;

    private UserEntity user;
    private WarehouseEntity warehouse;
    private OrderEntity order;

    @BeforeEach
    void setUp() {
        user = userRepository.save(TestFixtures.user("fulfilluser", Role.WAREHOUSE_STAFF));
        CategoryEntity category = categoryRepository.save(TestFixtures.category("Gadgets"));
        ProductEntity product = productRepository.save(
                TestFixtures.product("Phone", new BigDecimal("699.99"), category, user));

        warehouse = warehouseRepository.save(WarehouseEntity.builder()
                .name("Fulfillment Center")
                .code("FC-001")
                .location("Dallas")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build());

        LocalDateTime now = LocalDateTime.now();
        order = orderRepository.save(OrderEntity.builder()
                .buyer(user)
                .product(product)
                .quantity(1)
                .totalPrice(new BigDecimal("699.99"))
                .status(OrderStatus.CONFIRMED)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    @Test
    @DisplayName("create with same idempotency key returns the same fulfillment")
    void testCreate_idempotent() {
        Fulfillment first = fulfillmentService.create(order.getId(), warehouse.getId(), "idem-key-1");
        Fulfillment second = fulfillmentService.create(order.getId(), warehouse.getId(), "idem-key-1");

        assertEquals(first.getId(), second.getId());
    }

    @Test
    @DisplayName("advanceStep through PICK, PACK, SHIP, DELIVER completes the workflow")
    void testAdvanceStep_pickToPackToShipToDeliver() {
        Fulfillment fulfillment = fulfillmentService.create(order.getId(), warehouse.getId(), "idem-flow");

        Fulfillment afterPick = fulfillmentService.advanceStep(fulfillment.getId(), "PICK", user.getId(), "Picked items");
        assertEquals(FulfillmentStatus.PICKING, afterPick.getStatus());

        Fulfillment afterPack = fulfillmentService.advanceStep(fulfillment.getId(), "PACK", user.getId(), "Packed items");
        assertEquals(FulfillmentStatus.PACKING, afterPack.getStatus());

        Fulfillment afterShip = fulfillmentService.advanceStep(fulfillment.getId(), "SHIP", user.getId(), "Shipped");
        assertEquals(FulfillmentStatus.SHIPPED, afterShip.getStatus());

        Fulfillment afterDeliver = fulfillmentService.advanceStep(fulfillment.getId(), "DELIVER", user.getId(), "Delivered");
        assertEquals(FulfillmentStatus.DELIVERED, afterDeliver.getStatus());
    }

    @Test
    @DisplayName("advanceStep sets the correct fulfillment status for each step")
    void testAdvanceStep_setsCorrectFulfillmentStatus() {
        Fulfillment fulfillment = fulfillmentService.create(order.getId(), warehouse.getId(), "idem-status");

        Fulfillment afterPick = fulfillmentService.advanceStep(fulfillment.getId(), "PICK", user.getId(), null);
        assertEquals(FulfillmentStatus.PICKING, afterPick.getStatus());

        Fulfillment afterPack = fulfillmentService.advanceStep(fulfillment.getId(), "PACK", user.getId(), null);
        assertEquals(FulfillmentStatus.PACKING, afterPack.getStatus());
    }

    @Test
    @DisplayName("cancelFulfillment from PENDING succeeds")
    void testCancelFulfillment_fromPending_succeeds() {
        Fulfillment fulfillment = fulfillmentService.create(order.getId(), warehouse.getId(), "idem-cancel");

        Fulfillment cancelled = fulfillmentService.cancelFulfillment(fulfillment.getId());

        assertEquals(FulfillmentStatus.CANCELLED, cancelled.getStatus());
    }

    @Test
    @DisplayName("cancelFulfillment from SHIPPED throws RuntimeException")
    void testCancelFulfillment_fromShipped_throws() {
        Fulfillment fulfillment = fulfillmentService.create(order.getId(), warehouse.getId(), "idem-cancel-ship");

        fulfillmentService.advanceStep(fulfillment.getId(), "PICK", user.getId(), null);
        fulfillmentService.advanceStep(fulfillment.getId(), "PACK", user.getId(), null);
        fulfillmentService.advanceStep(fulfillment.getId(), "SHIP", user.getId(), null);

        assertThrows(RuntimeException.class,
                () -> fulfillmentService.cancelFulfillment(fulfillment.getId()));
    }

    @Test
    @DisplayName("advanceStep creates a FulfillmentStepEntity record")
    void testAdvanceStep_createsStepRecord() {
        Fulfillment fulfillment = fulfillmentService.create(order.getId(), warehouse.getId(), "idem-step");

        fulfillmentService.advanceStep(fulfillment.getId(), "PICK", user.getId(), "Step note");

        List<FulfillmentStepEntity> steps = fulfillmentStepRepository
                .findByFulfillmentIdOrderByCreatedAtAsc(fulfillment.getId());

        assertEquals(1, steps.size());
        assertEquals("PICK", steps.get(0).getStepName());
        assertEquals("COMPLETED", steps.get(0).getStatus());
        assertEquals(user.getId(), steps.get(0).getOperatorId());
        assertEquals("Step note", steps.get(0).getNotes());
    }

    @Test
    @DisplayName("getSteps returns steps sorted by createdAt ascending")
    void testGetSteps_returnsSortedByCreatedAt() {
        Fulfillment fulfillment = fulfillmentService.create(order.getId(), warehouse.getId(), "idem-sorted");

        fulfillmentService.advanceStep(fulfillment.getId(), "PICK", user.getId(), null);
        fulfillmentService.advanceStep(fulfillment.getId(), "PACK", user.getId(), null);
        fulfillmentService.advanceStep(fulfillment.getId(), "SHIP", user.getId(), null);

        List<FulfillmentStep> steps = fulfillmentService.getSteps(fulfillment.getId());

        assertEquals(3, steps.size());
        assertEquals("PICK", steps.get(0).getStepName().name());
        assertEquals("PACK", steps.get(1).getStepName().name());
        assertEquals("SHIP", steps.get(2).getStepName().name());
    }
}

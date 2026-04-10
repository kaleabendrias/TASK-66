package com.demo.app.application.service;

import com.demo.app.DemoApplication;
import com.demo.app.TestFixtures;
import com.demo.app.domain.enums.OrderStatus;
import com.demo.app.domain.enums.Role;
import com.demo.app.domain.exception.ResourceNotFoundException;
import com.demo.app.domain.model.Order;
import com.demo.app.persistence.entity.CategoryEntity;
import com.demo.app.persistence.entity.OrderEntity;
import com.demo.app.persistence.entity.ProductEntity;
import com.demo.app.persistence.entity.UserEntity;
import com.demo.app.persistence.repository.CategoryRepository;
import com.demo.app.persistence.repository.OrderRepository;
import com.demo.app.persistence.repository.ProductRepository;
import com.demo.app.persistence.repository.UserRepository;
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

    private UserEntity buyer;
    private ProductEntity product;
    private OrderEntity order;

    @BeforeEach
    void setUp() {
        buyer = userRepository.save(TestFixtures.user("ordsvc_buyer", Role.MEMBER));
        UserEntity seller = userRepository.save(TestFixtures.user("ordsvc_seller", Role.SELLER));
        CategoryEntity category = categoryRepository.save(TestFixtures.category("Books"));
        product = productRepository.save(TestFixtures.product("Novel", BigDecimal.valueOf(15), category, seller));

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
}

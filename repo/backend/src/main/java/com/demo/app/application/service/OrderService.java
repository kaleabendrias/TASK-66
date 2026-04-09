package com.demo.app.application.service;

import com.demo.app.domain.enums.OrderStatus;
import com.demo.app.domain.model.Order;
import com.demo.app.persistence.entity.OrderEntity;
import com.demo.app.persistence.entity.ProductEntity;
import com.demo.app.persistence.entity.UserEntity;
import com.demo.app.persistence.repository.OrderRepository;
import com.demo.app.persistence.repository.ProductRepository;
import com.demo.app.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<Order> getAll() {
        return orderRepository.findAll().stream()
                .map(OrderEntity::toModel)
                .toList();
    }

    @Transactional(readOnly = true)
    public Order getById(Long id) {
        return orderRepository.findById(id)
                .map(OrderEntity::toModel)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Order> getByBuyer(Long buyerId) {
        return orderRepository.findByBuyer_Id(buyerId).stream()
                .map(OrderEntity::toModel)
                .toList();
    }

    @Transactional
    public Order placeOrder(Order order) {
        UserEntity buyer = userRepository.findById(order.getBuyerId())
                .orElseThrow(() -> new RuntimeException("Buyer not found with id: " + order.getBuyerId()));
        ProductEntity product = productRepository.findById(order.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + order.getProductId()));

        OrderEntity entity = OrderEntity.builder()
                .buyer(buyer)
                .product(product)
                .quantity(order.getQuantity())
                .totalPrice(order.getTotalPrice())
                .status(OrderStatus.PLACED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return orderRepository.save(entity).toModel();
    }

    @Transactional
    public Order updateStatus(Long id, OrderStatus status) {
        OrderEntity entity = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
        entity.setStatus(status);
        entity.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(entity).toModel();
    }
}

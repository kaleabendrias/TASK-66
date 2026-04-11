package com.demo.app.api.controller;

import com.demo.app.infrastructure.audit.Audited;
import com.demo.app.api.dto.OrderDto;
import com.demo.app.application.service.OrderService;
import com.demo.app.domain.enums.OrderStatus;
import com.demo.app.domain.exception.OwnershipViolationException;
import com.demo.app.domain.model.Order;
import com.demo.app.persistence.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'MODERATOR', 'ADMINISTRATOR')")
    public ResponseEntity<List<OrderDto>> getAll() {
        List<OrderDto> orders = orderService.getAll().stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getById(@PathVariable Long id) {
        Order order = orderService.getById(id);
        enforceOwnership(order.getBuyerId());
        return ResponseEntity.ok(toDto(order));
    }

    @GetMapping("/buyer/{buyerId}")
    public ResponseEntity<List<OrderDto>> getByBuyer(@PathVariable Long buyerId) {
        enforceOwnership(buyerId);
        List<OrderDto> orders = orderService.getByBuyer(buyerId).stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(orders);
    }

    @PostMapping
    @Audited(entityType = "ORDER", action = "PLACE")
    public ResponseEntity<OrderDto> placeOrder(@Valid @RequestBody OrderDto dto) {
        Long authenticatedUserId = getCurrentUserId();
        Order placed = orderService.placeOrder(
                authenticatedUserId,
                dto.productId(),
                dto.quantity(),
                dto.totalPrice(),
                dto.reservationId(),
                dto.inventoryItemId(),
                dto.idempotencyKey());
        BigDecimal clientTotal = dto.totalPrice();
        if (clientTotal != null && placed.getTotalPrice() != null
                && clientTotal.compareTo(placed.getTotalPrice()) != 0) {
            // Visible signal that a client tried to influence price; the
            // server already overrode it but we want this in the logs so
            // operators can spot tampering attempts.
            log.warn("Order {}: client-supplied total {} ignored, server resolved {}",
                    placed.getId(), clientTotal, placed.getTotalPrice());
        }
        return ResponseEntity.ok(toDto(placed));
    }

    @PatchMapping("/{id}/status")
    @Audited(entityType = "ORDER", action = "STATUS_CHANGE")
    public ResponseEntity<OrderDto> updateStatus(@PathVariable Long id, @RequestParam OrderStatus status) {
        Order order = orderService.getById(id);

        // Buyers can only cancel their own orders
        if (!isPrivileged()) {
            enforceOwnership(order.getBuyerId());
            if (status != OrderStatus.CANCELLED) {
                throw new OwnershipViolationException("Buyers can only cancel orders. Status changes to " + status + " require staff privileges.");
            }
        }

        Long actorId = getCurrentUserId();
        return ResponseEntity.ok(toDto(orderService.updateStatus(id, status, actorId)));
    }

    private void enforceOwnership(Long ownerId) {
        if (isPrivileged()) return;
        Long currentUserId = getCurrentUserId();
        if (!currentUserId.equals(ownerId)) {
            throw new OwnershipViolationException("You do not have access to this resource");
        }
    }

    private boolean isPrivileged() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMINISTRATOR") ||
                a.getAuthority().equals("ROLE_MODERATOR") ||
                a.getAuthority().equals("ROLE_WAREHOUSE_STAFF"));
    }

    private Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }

    private OrderDto toDto(Order order) {
        return new OrderDto(
                order.getId(),
                order.getBuyerId(),
                order.getProductId(),
                order.getQuantity(),
                order.getTotalPrice(),
                order.getStatus() != null ? order.getStatus().name() : null,
                order.getTenderType(),
                order.getRefundAmount(),
                order.getRefundReason(),
                order.isReconciled(),
                order.getReconciledAt(),
                order.getReconciliationRef(),
                order.getReservationId(),
                null,
                null
        );
    }
}

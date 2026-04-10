package com.demo.app.api.controller;

import com.demo.app.api.dto.AdvanceStepRequest;
import com.demo.app.api.dto.CreateFulfillmentRequest;
import com.demo.app.api.dto.FulfillmentDto;
import com.demo.app.api.dto.FulfillmentStepDto;
import com.demo.app.application.service.FulfillmentService;
import com.demo.app.application.service.OrderService;
import com.demo.app.application.service.ProductService;
import com.demo.app.domain.model.Fulfillment;
import com.demo.app.domain.model.FulfillmentStep;
import com.demo.app.domain.exception.OwnershipViolationException;
import com.demo.app.persistence.entity.UserEntity;
import com.demo.app.persistence.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fulfillments")
@RequiredArgsConstructor
public class FulfillmentController {

    private final FulfillmentService fulfillmentService;
    private final OrderService orderService;
    private final ProductService productService;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'ADMINISTRATOR')")
    public ResponseEntity<FulfillmentDto> create(@Valid @RequestBody CreateFulfillmentRequest request) {
        Fulfillment fulfillment = fulfillmentService.create(
                request.orderId(), request.warehouseId(), request.idempotencyKey());
        return ResponseEntity.ok(toDto(fulfillment));
    }

    @PostMapping("/{id}/advance")
    public ResponseEntity<FulfillmentDto> advanceStep(@PathVariable Long id, @RequestBody AdvanceStepRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // Verify the operator is assigned to this fulfillment (or is admin)
        Fulfillment existing = fulfillmentService.getById(id);
        if (existing.getOperatorId() != null && !existing.getOperatorId().equals(user.getId()) && !isAdmin()) {
            throw new OwnershipViolationException("You are not assigned to this fulfillment");
        }

        Fulfillment fulfillment = fulfillmentService.advanceStep(id, request.stepName(), user.getId(), request.notes());
        return ResponseEntity.ok(toDto(fulfillment));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<FulfillmentDto> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(toDto(fulfillmentService.cancelFulfillment(id)));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<FulfillmentDto> getByOrder(@PathVariable Long orderId) {
        if (isSeller()) {
            com.demo.app.domain.model.Order order = orderService.getById(orderId);
            com.demo.app.domain.model.Product product = productService.getById(order.getProductId());
            if (!product.getSellerId().equals(getCurrentUserId())) {
                throw new OwnershipViolationException("Sellers can only access fulfillments for their own products");
            }
        }
        return ResponseEntity.ok(toDto(fulfillmentService.getByOrder(orderId)));
    }

    @GetMapping("/{id}/steps")
    public ResponseEntity<List<FulfillmentStepDto>> getSteps(@PathVariable Long id) {
        // Seller scoping: verify fulfillment relates to seller's product
        if (isSeller()) {
            com.demo.app.domain.model.Fulfillment ful = fulfillmentService.getById(id);
            com.demo.app.domain.model.Order order = orderService.getById(ful.getOrderId());
            com.demo.app.domain.model.Product product = productService.getById(order.getProductId());
            if (!product.getSellerId().equals(getCurrentUserId())) {
                throw new com.demo.app.domain.exception.OwnershipViolationException(
                        "Sellers can only view fulfillment steps for their own products");
            }
        }
        List<FulfillmentStepDto> steps = fulfillmentService.getSteps(id).stream()
                .map(this::toStepDto)
                .toList();
        return ResponseEntity.ok(steps);
    }

    private boolean isSeller() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SELLER"))
                && auth.getAuthorities().stream().noneMatch(a ->
                        a.getAuthority().equals("ROLE_WAREHOUSE_STAFF") || a.getAuthority().equals("ROLE_ADMINISTRATOR"));
    }

    private Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username))
                .getId();
    }

    private boolean isAdmin() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRATOR"));
    }

    private FulfillmentDto toDto(Fulfillment f) {
        return new FulfillmentDto(f.getId(), f.getOrderId(), f.getWarehouseId(),
                f.getStatus() != null ? f.getStatus().name() : null,
                f.getOperatorId(), f.getTrackingInfo(), f.getIdempotencyKey());
    }

    private FulfillmentStepDto toStepDto(FulfillmentStep s) {
        return new FulfillmentStepDto(s.getId(), s.getFulfillmentId(),
                s.getStepName() != null ? s.getStepName().name() : null,
                s.getStatus(), s.getOperatorId(), s.getNotes(), s.getCreatedAt(), s.getCompletedAt());
    }
}

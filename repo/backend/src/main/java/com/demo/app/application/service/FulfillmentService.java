package com.demo.app.application.service;

import com.demo.app.domain.exception.ResourceNotFoundException;
import com.demo.app.domain.enums.FulfillmentStatus;
import com.demo.app.domain.enums.FulfillmentStepName;
import com.demo.app.domain.enums.MovementType;
import com.demo.app.domain.model.Fulfillment;
import com.demo.app.domain.model.FulfillmentStep;
import com.demo.app.infrastructure.SystemOperatorProvider;
import com.demo.app.infrastructure.audit.AuditService;
import com.demo.app.persistence.entity.FulfillmentEntity;
import com.demo.app.persistence.entity.FulfillmentStepEntity;
import com.demo.app.persistence.entity.InventoryItemEntity;
import com.demo.app.persistence.entity.InventoryMovementEntity;
import com.demo.app.persistence.entity.OrderEntity;
import com.demo.app.persistence.repository.FulfillmentRepository;
import com.demo.app.persistence.repository.FulfillmentStepRepository;
import com.demo.app.persistence.repository.InventoryItemRepository;
import com.demo.app.persistence.repository.InventoryMovementRepository;
import com.demo.app.persistence.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class FulfillmentService {

    private final FulfillmentRepository fulfillmentRepository;
    private final FulfillmentStepRepository fulfillmentStepRepository;
    private final OrderRepository orderRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final AuditService auditService;
    private final SystemOperatorProvider systemOperatorProvider;

    private static final Map<FulfillmentStepName, FulfillmentStatus> STEP_TO_STATUS = Map.of(
            FulfillmentStepName.PICK, FulfillmentStatus.PICKING,
            FulfillmentStepName.PACK, FulfillmentStatus.PACKING,
            FulfillmentStepName.SHIP, FulfillmentStatus.SHIPPED,
            FulfillmentStepName.DELIVER, FulfillmentStatus.DELIVERED
    );

    private static final Map<FulfillmentStepName, Set<FulfillmentStatus>> ALLOWED_TRANSITIONS = Map.of(
            FulfillmentStepName.PICK, Set.of(FulfillmentStatus.PENDING),
            FulfillmentStepName.PACK, Set.of(FulfillmentStatus.PICKING),
            FulfillmentStepName.SHIP, Set.of(FulfillmentStatus.PACKING),
            FulfillmentStepName.DELIVER, Set.of(FulfillmentStatus.SHIPPED)
    );

    public Fulfillment create(Long orderId, Long warehouseId, String idempotencyKey) {
        Optional<FulfillmentEntity> existing = fulfillmentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get().toModel();
        }

        LocalDateTime now = LocalDateTime.now();
        FulfillmentEntity entity = FulfillmentEntity.builder()
                .orderId(orderId)
                .warehouseId(warehouseId)
                .status(FulfillmentStatus.PENDING.name())
                .idempotencyKey(idempotencyKey)
                .createdAt(now)
                .updatedAt(now)
                .build();
        return fulfillmentRepository.save(entity).toModel();
    }

    public Fulfillment advanceStep(Long fulfillmentId, String stepName, Long operatorId, String notes) {
        FulfillmentEntity fulfillment = fulfillmentRepository.findById(fulfillmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Fulfillment", fulfillmentId));

        FulfillmentStepName step = FulfillmentStepName.valueOf(stepName);
        FulfillmentStatus currentStatus = FulfillmentStatus.valueOf(fulfillment.getStatus());

        Set<FulfillmentStatus> allowed = ALLOWED_TRANSITIONS.get(step);
        if (allowed == null || !allowed.contains(currentStatus)) {
            throw new IllegalStateException("Cannot perform step " + stepName + " when fulfillment status is " + currentStatus);
        }

        LocalDateTime now = LocalDateTime.now();
        FulfillmentStepEntity stepEntity = FulfillmentStepEntity.builder()
                .fulfillmentId(fulfillmentId)
                .stepName(step.name())
                .status("COMPLETED")
                .operatorId(operatorId)
                .notes(notes)
                .createdAt(now)
                .completedAt(now)
                .build();
        fulfillmentStepRepository.save(stepEntity);

        FulfillmentStatus newStatus = STEP_TO_STATUS.get(step);
        fulfillment.setStatus(newStatus.name());
        fulfillment.setUpdatedAt(now);
        fulfillmentRepository.save(fulfillment);

        return fulfillment.toModel();
    }

    /**
     * Cancel a fulfillment and run the full compensating transaction:
     * <ol>
     *     <li>Restore inventory quantities for the order's product at this warehouse</li>
     *     <li>Write an immutable RETURN movement to the inventory_movement log</li>
     *     <li>Emit an AUDIT entry recording the rollback</li>
     *     <li>Mark the fulfillment as CANCELLED</li>
     * </ol>
     * Each step runs in the same transaction so a failure rolls everything back —
     * we never want a CANCELLED fulfillment without its compensation, or a
     * compensation movement without the corresponding state change.
     */
    public Fulfillment cancelFulfillment(Long fulfillmentId, Long actorUserId, String reason) {
        FulfillmentEntity fulfillment = fulfillmentRepository.findById(fulfillmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Fulfillment", fulfillmentId));

        FulfillmentStatus currentStatus = FulfillmentStatus.valueOf(fulfillment.getStatus());
        if (currentStatus == FulfillmentStatus.CANCELLED) {
            return fulfillment.toModel();
        }
        if (currentStatus == FulfillmentStatus.DELIVERED) {
            throw new IllegalStateException(
                    "Delivered fulfillments cannot be cancelled — issue a RETURN instead. Current: " + currentStatus);
        }

        OrderEntity order = orderRepository.findById(fulfillment.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", fulfillment.getOrderId()));
        int orderQuantity = order.getQuantity();
        Long productId = order.getProduct() != null ? order.getProduct().getId() : order.getProductId();

        InventoryItemEntity item = inventoryItemRepository
                .findByProductIdAndWarehouseId(productId, fulfillment.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory item for product/warehouse",
                        fulfillment.getWarehouseId()));

        LocalDateTime now = LocalDateTime.now();

        // Compensate: restore on-hand for not-yet-shipped fulfillments. PICKING /
        // PACKING / SHIPPED have all deducted on_hand at confirm-time, so the
        // rollback adds it back. PENDING never deducted, so we still log the
        // movement (with quantity 0) so the audit trail captures the cancel
        // event uniformly.
        boolean inventoryDeducted =
                currentStatus == FulfillmentStatus.PICKING
                        || currentStatus == FulfillmentStatus.PACKING
                        || currentStatus == FulfillmentStatus.SHIPPED;
        int restoredQty = inventoryDeducted ? orderQuantity : 0;
        int balanceBefore = item.getQuantityOnHand();
        if (inventoryDeducted) {
            item.setQuantityOnHand(item.getQuantityOnHand() + restoredQty);
            item.setUpdatedAt(now);
            inventoryItemRepository.save(item);
        }

        Long operatorId = actorUserId != null ? actorUserId : systemOperatorProvider.getSystemOperatorId();

        InventoryMovementEntity compensation = InventoryMovementEntity.builder()
                .inventoryItemId(item.getId())
                .warehouseId(item.getWarehouseId())
                .movementType(MovementType.RETURN.name())
                .quantity(restoredQty)
                .balanceAfter(item.getQuantityOnHand())
                .referenceDocument("fulfillment-cancel:" + fulfillmentId)
                .operatorId(operatorId)
                .notes("Compensation rollback for cancelled fulfillment "
                        + fulfillmentId + (reason != null ? " — " + reason : ""))
                .createdAt(now)
                .build();
        inventoryMovementRepository.save(compensation);

        Map<String, Object> auditOld = new HashMap<>();
        auditOld.put("status", currentStatus.name());
        auditOld.put("balance_before", balanceBefore);
        Map<String, Object> auditNew = new HashMap<>();
        auditNew.put("status", FulfillmentStatus.CANCELLED.name());
        auditNew.put("balance_after", item.getQuantityOnHand());
        auditNew.put("restored_quantity", restoredQty);
        auditNew.put("reason", reason);
        auditService.log(
                "FULFILLMENT",
                fulfillmentId,
                "CANCEL_COMPENSATED",
                operatorId,
                auditOld,
                auditNew,
                null);

        fulfillment.setStatus(FulfillmentStatus.CANCELLED.name());
        fulfillment.setUpdatedAt(now);
        FulfillmentEntity saved = fulfillmentRepository.save(fulfillment);

        log.info("Cancelled fulfillment {} (was {}): restored {} units of product {} at warehouse {}",
                fulfillmentId, currentStatus, restoredQty, productId, fulfillment.getWarehouseId());

        return saved.toModel();
    }

    public Fulfillment getById(Long id) {
        return fulfillmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fulfillment", id))
                .toModel();
    }

    public Fulfillment getByOrder(Long orderId) {
        return fulfillmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Fulfillment for order", orderId))
                .toModel();
    }

    public List<FulfillmentStep> getSteps(Long fulfillmentId) {
        return fulfillmentStepRepository.findByFulfillmentIdOrderByCreatedAtAsc(fulfillmentId).stream()
                .map(FulfillmentStepEntity::toModel)
                .toList();
    }
}

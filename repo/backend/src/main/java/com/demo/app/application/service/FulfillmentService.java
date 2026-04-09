package com.demo.app.application.service;

import com.demo.app.domain.exception.ResourceNotFoundException;
import com.demo.app.domain.enums.FulfillmentStatus;
import com.demo.app.domain.enums.FulfillmentStepName;
import com.demo.app.domain.model.Fulfillment;
import com.demo.app.domain.model.FulfillmentStep;
import com.demo.app.persistence.entity.FulfillmentEntity;
import com.demo.app.persistence.entity.FulfillmentStepEntity;
import com.demo.app.persistence.repository.FulfillmentRepository;
import com.demo.app.persistence.repository.FulfillmentStepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class FulfillmentService {

    private final FulfillmentRepository fulfillmentRepository;
    private final FulfillmentStepRepository fulfillmentStepRepository;

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

    public Fulfillment cancelFulfillment(Long fulfillmentId) {
        FulfillmentEntity fulfillment = fulfillmentRepository.findById(fulfillmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Fulfillment", fulfillmentId));

        FulfillmentStatus currentStatus = FulfillmentStatus.valueOf(fulfillment.getStatus());
        if (currentStatus != FulfillmentStatus.PENDING && currentStatus != FulfillmentStatus.PICKING) {
            throw new IllegalStateException("Fulfillment can only be cancelled when PENDING or PICKING. Current: " + currentStatus);
        }

        fulfillment.setStatus(FulfillmentStatus.CANCELLED.name());
        fulfillment.setUpdatedAt(LocalDateTime.now());
        return fulfillmentRepository.save(fulfillment).toModel();
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

package com.demo.app.application.service;

import com.demo.app.domain.enums.MovementType;
import com.demo.app.domain.enums.ReservationStatus;
import com.demo.app.domain.model.StockReservation;
import com.demo.app.persistence.entity.InventoryItemEntity;
import com.demo.app.persistence.entity.InventoryMovementEntity;
import com.demo.app.persistence.entity.StockReservationEntity;
import com.demo.app.persistence.repository.InventoryItemRepository;
import com.demo.app.persistence.repository.InventoryMovementRepository;
import com.demo.app.persistence.repository.StockReservationRepository;
import com.demo.app.domain.exception.ResourceNotFoundException;
import com.demo.app.domain.exception.InsufficientStockException;
import com.demo.app.infrastructure.SystemOperatorProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReservationService {

    private final InventoryItemRepository inventoryItemRepository;
    private final StockReservationRepository stockReservationRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final SystemOperatorProvider systemOperatorProvider;

    @Value("${app.reservation.hold-minutes:30}")
    private int holdMinutes;

    public StockReservation getById(Long id) {
        return stockReservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", id))
                .toModel();
    }

    public StockReservation reserve(Long inventoryItemId, Long userId, int quantity, String idempotencyKey) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive, got: " + quantity);
        }

        Optional<StockReservationEntity> existing = stockReservationRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get().toModel();
        }

        InventoryItemEntity item = inventoryItemRepository.findById(inventoryItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item", inventoryItemId));

        int available = item.getQuantityOnHand() - item.getQuantityReserved();
        if (available < quantity) {
            throw new InsufficientStockException(available, quantity);
        }

        item.setQuantityReserved(item.getQuantityReserved() + quantity);
        item.setUpdatedAt(LocalDateTime.now());
        inventoryItemRepository.save(item);

        LocalDateTime now = LocalDateTime.now();
        StockReservationEntity reservation = StockReservationEntity.builder()
                .inventoryItemId(inventoryItemId)
                .userId(userId)
                .quantity(quantity)
                .status(ReservationStatus.HELD.name())
                .idempotencyKey(idempotencyKey)
                .expiresAt(now.plusMinutes(holdMinutes))
                .createdAt(now)
                .build();
        stockReservationRepository.save(reservation);

        InventoryMovementEntity movement = InventoryMovementEntity.builder()
                .inventoryItemId(inventoryItemId)
                .warehouseId(item.getWarehouseId())
                .movementType(MovementType.RESERVATION_HOLD.name())
                .quantity(quantity)
                .balanceAfter(item.getQuantityOnHand())
                .referenceDocument("reservation:" + reservation.getId())
                .operatorId(userId)
                .createdAt(now)
                .build();
        inventoryMovementRepository.save(movement);

        return reservation.toModel();
    }

    public StockReservation confirm(Long reservationId) {
        StockReservationEntity reservation = stockReservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", reservationId));

        if (ReservationStatus.CONFIRMED.name().equals(reservation.getStatus())) {
            return reservation.toModel();
        }

        if (!ReservationStatus.HELD.name().equals(reservation.getStatus())) {
            throw new IllegalStateException("Reservation cannot be confirmed. Current status: " + reservation.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        reservation.setStatus(ReservationStatus.CONFIRMED.name());
        reservation.setConfirmedAt(now);
        stockReservationRepository.save(reservation);

        InventoryItemEntity item = inventoryItemRepository.findById(reservation.getInventoryItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item", reservation.getInventoryItemId()));

        item.setQuantityOnHand(item.getQuantityOnHand() - reservation.getQuantity());
        item.setQuantityReserved(item.getQuantityReserved() - reservation.getQuantity());
        item.setUpdatedAt(now);
        inventoryItemRepository.save(item);

        InventoryMovementEntity movement = InventoryMovementEntity.builder()
                .inventoryItemId(item.getId())
                .warehouseId(item.getWarehouseId())
                .movementType(MovementType.SHIPMENT.name())
                .quantity(-reservation.getQuantity())
                .balanceAfter(item.getQuantityOnHand())
                .referenceDocument("reservation:" + reservationId)
                .operatorId(reservation.getUserId())
                .createdAt(now)
                .build();
        inventoryMovementRepository.save(movement);

        return reservation.toModel();
    }

    public StockReservation cancel(Long reservationId) {
        StockReservationEntity reservation = stockReservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", reservationId));

        if (ReservationStatus.CANCELLED.name().equals(reservation.getStatus())
                || ReservationStatus.EXPIRED.name().equals(reservation.getStatus())) {
            return reservation.toModel();
        }

        if (!ReservationStatus.HELD.name().equals(reservation.getStatus())) {
            throw new IllegalStateException("Reservation cannot be cancelled. Current status: " + reservation.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        reservation.setStatus(ReservationStatus.CANCELLED.name());
        reservation.setCancelledAt(now);
        stockReservationRepository.save(reservation);

        InventoryItemEntity item = inventoryItemRepository.findById(reservation.getInventoryItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item", reservation.getInventoryItemId()));

        item.setQuantityReserved(item.getQuantityReserved() - reservation.getQuantity());
        item.setUpdatedAt(now);
        inventoryItemRepository.save(item);

        InventoryMovementEntity movement = InventoryMovementEntity.builder()
                .inventoryItemId(item.getId())
                .warehouseId(item.getWarehouseId())
                .movementType(MovementType.RESERVATION_RELEASE.name())
                .quantity(reservation.getQuantity())
                .balanceAfter(item.getQuantityOnHand())
                .referenceDocument("reservation:" + reservationId)
                .operatorId(reservation.getUserId())
                .createdAt(now)
                .build();
        inventoryMovementRepository.save(movement);

        return reservation.toModel();
    }

    public void expireOverdueReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<StockReservationEntity> expired = stockReservationRepository.findExpired(now);
        if (expired.isEmpty()) {
            return;
        }

        // Resolve once per batch — the system operator id is invariant.
        // inventory_movement.operator_id is NOT NULL REFERENCES app_user(id),
        // so a null here would crash the entire scheduler tick.
        Long systemOperatorId = systemOperatorProvider.getSystemOperatorId();

        for (StockReservationEntity reservation : expired) {
            reservation.setStatus(ReservationStatus.EXPIRED.name());
            reservation.setCancelledAt(now);
            stockReservationRepository.save(reservation);

            InventoryItemEntity item = inventoryItemRepository.findById(reservation.getInventoryItemId())
                    .orElse(null);
            if (item == null) {
                continue;
            }
            item.setQuantityReserved(item.getQuantityReserved() - reservation.getQuantity());
            item.setUpdatedAt(now);
            inventoryItemRepository.save(item);

            InventoryMovementEntity movement = InventoryMovementEntity.builder()
                    .inventoryItemId(item.getId())
                    .warehouseId(item.getWarehouseId())
                    .movementType(MovementType.RESERVATION_RELEASE.name())
                    .quantity(reservation.getQuantity())
                    .balanceAfter(item.getQuantityOnHand())
                    .referenceDocument("reservation-expired:" + reservation.getId())
                    .operatorId(systemOperatorId)
                    .notes("Auto-expired by scheduler at " + now)
                    .createdAt(now)
                    .build();
            inventoryMovementRepository.save(movement);
        }

        log.info("Expired {} overdue reservations", expired.size());
    }

    /**
     * Compensating rollback for a reservation that was already CONFIRMED
     * (i.e. on_hand was already deducted at confirm time). Adds the quantity
     * back to on_hand, marks the reservation CANCELLED, and writes a RETURN
     * movement so the audit trail captures the rollback.
     *
     * <p>Used by OrderService when an order is CANCELLED or FAILED after
     * confirmation but before shipment leaves the building.
     */
    public StockReservation rollbackConfirmed(Long reservationId, Long actorUserId) {
        StockReservationEntity reservation = stockReservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", reservationId));
        if (ReservationStatus.CANCELLED.name().equals(reservation.getStatus())) {
            return reservation.toModel();
        }
        if (!ReservationStatus.CONFIRMED.name().equals(reservation.getStatus())) {
            throw new IllegalStateException(
                    "rollbackConfirmed only applies to CONFIRMED reservations. Current: " + reservation.getStatus());
        }

        InventoryItemEntity item = inventoryItemRepository.findById(reservation.getInventoryItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item", reservation.getInventoryItemId()));

        LocalDateTime now = LocalDateTime.now();
        item.setQuantityOnHand(item.getQuantityOnHand() + reservation.getQuantity());
        item.setUpdatedAt(now);
        inventoryItemRepository.save(item);

        reservation.setStatus(ReservationStatus.CANCELLED.name());
        reservation.setCancelledAt(now);
        stockReservationRepository.save(reservation);

        Long operatorId = actorUserId != null ? actorUserId : systemOperatorProvider.getSystemOperatorId();
        InventoryMovementEntity movement = InventoryMovementEntity.builder()
                .inventoryItemId(item.getId())
                .warehouseId(item.getWarehouseId())
                .movementType(MovementType.RETURN.name())
                .quantity(reservation.getQuantity())
                .balanceAfter(item.getQuantityOnHand())
                .referenceDocument("reservation-rollback:" + reservationId)
                .operatorId(operatorId)
                .notes("Compensation rollback for cancelled/failed order")
                .createdAt(now)
                .build();
        inventoryMovementRepository.save(movement);

        return reservation.toModel();
    }

    public List<StockReservation> getUserHeldReservations(Long userId) {
        return stockReservationRepository.findByUserIdAndStatus(userId, ReservationStatus.HELD.name()).stream()
                .map(StockReservationEntity::toModel)
                .toList();
    }
}

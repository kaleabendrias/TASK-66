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

        for (StockReservationEntity reservation : expired) {
            reservation.setStatus(ReservationStatus.EXPIRED.name());
            reservation.setCancelledAt(now);
            stockReservationRepository.save(reservation);

            InventoryItemEntity item = inventoryItemRepository.findById(reservation.getInventoryItemId())
                    .orElse(null);
            if (item != null) {
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
                        .createdAt(now)
                        .build();
                inventoryMovementRepository.save(movement);
            }
        }

        if (!expired.isEmpty()) {
            log.info("Expired {} overdue reservations", expired.size());
        }
    }

    public List<StockReservation> getUserHeldReservations(Long userId) {
        return stockReservationRepository.findByUserIdAndStatus(userId, ReservationStatus.HELD.name()).stream()
                .map(StockReservationEntity::toModel)
                .toList();
    }
}

package com.demo.app.persistence.entity;

import com.demo.app.domain.enums.ReservationStatus;
import com.demo.app.domain.model.StockReservation;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_reservation")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReservationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inventory_item_id", nullable = false)
    private Long inventoryItemId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    public StockReservation toModel() {
        return StockReservation.builder()
                .id(id)
                .inventoryItemId(inventoryItemId)
                .userId(userId)
                .quantity(quantity)
                .status(ReservationStatus.valueOf(status))
                .idempotencyKey(idempotencyKey)
                .expiresAt(expiresAt)
                .createdAt(createdAt)
                .confirmedAt(confirmedAt)
                .cancelledAt(cancelledAt)
                .build();
    }
}

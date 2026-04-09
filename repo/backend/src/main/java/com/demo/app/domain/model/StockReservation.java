package com.demo.app.domain.model;

import com.demo.app.domain.enums.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReservation {
    private Long id;
    private Long inventoryItemId;
    private Long userId;
    private int quantity;
    private ReservationStatus status;
    private String idempotencyKey;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime cancelledAt;
}

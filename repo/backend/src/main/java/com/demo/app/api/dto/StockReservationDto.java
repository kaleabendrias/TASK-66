package com.demo.app.api.dto;

import java.time.LocalDateTime;

public record StockReservationDto(Long id, Long inventoryItemId, Long userId, int quantity, String status,
                                   String idempotencyKey, LocalDateTime expiresAt, LocalDateTime createdAt,
                                   LocalDateTime confirmedAt, LocalDateTime cancelledAt) {
}

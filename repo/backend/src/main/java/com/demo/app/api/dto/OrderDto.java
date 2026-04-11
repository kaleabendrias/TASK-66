package com.demo.app.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Order request and response payload.
 *
 * <p>{@code totalPrice}, {@code status}, {@code refund*}, {@code reconciled*},
 * {@code id} and {@code buyerId} are response-only fields. They are still
 * present on the inbound payload for symmetry, but the controller never
 * trusts {@code totalPrice} from the client — see {@code PricingService} for
 * the authoritative computation. The {@code @DecimalMin} only catches
 * obviously bogus negative numbers; the real spoofing protection is the
 * server-side recompute.
 *
 * <p>Validation:
 * <ul>
 *     <li>{@code productId} must be present and positive</li>
 *     <li>{@code quantity} must be at least 1</li>
 *     <li>{@code totalPrice}, if provided, must be non-negative</li>
 * </ul>
 */
public record OrderDto(
        Long id,
        Long buyerId,
        @NotNull(message = "productId is required")
        @Positive(message = "productId must be a positive id")
        Long productId,
        @Min(value = 1, message = "quantity must be at least 1")
        int quantity,
        @DecimalMin(value = "0.00", inclusive = true, message = "totalPrice must be non-negative")
        BigDecimal totalPrice,
        String status,
        String tenderType,
        BigDecimal refundAmount,
        String refundReason,
        boolean reconciled,
        LocalDateTime reconciledAt,
        String reconciliationRef,
        // Optional: caller can pre-create a HELD reservation and bind it.
        Long reservationId,
        // Optional hint when no reservationId is provided — pick this
        // inventory row to mint the hold against. Falls back to first
        // available row for the product when null.
        Long inventoryItemId,
        // Optional idempotency key for the auto-created reservation. Falls
        // back to a generated key when null.
        String idempotencyKey
) {}

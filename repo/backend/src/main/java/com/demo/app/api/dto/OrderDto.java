package com.demo.app.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderDto(
        Long id,
        Long buyerId,
        Long productId,
        int quantity,
        BigDecimal totalPrice,
        String status,
        String tenderType,
        BigDecimal refundAmount,
        String refundReason,
        boolean reconciled,
        LocalDateTime reconciledAt,
        String reconciliationRef
) {}

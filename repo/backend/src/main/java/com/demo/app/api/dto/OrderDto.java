package com.demo.app.api.dto;

import java.math.BigDecimal;

public record OrderDto(
        Long id,
        Long buyerId,
        Long productId,
        int quantity,
        BigDecimal totalPrice,
        String status
) {}

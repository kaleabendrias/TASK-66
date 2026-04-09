package com.demo.app.api.dto;

import java.math.BigDecimal;

public record ProductDto(
        Long id,
        String name,
        String description,
        BigDecimal price,
        int stockQuantity,
        Long categoryId,
        String categoryName,
        Long sellerId,
        String sellerName,
        String status
) {}

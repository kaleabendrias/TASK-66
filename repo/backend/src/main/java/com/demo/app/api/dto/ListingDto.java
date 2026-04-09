package com.demo.app.api.dto;

import java.time.LocalDateTime;

public record ListingDto(Long id, Long productId, String title, String slug, String summary,
                          String[] tags, boolean featured, long viewCount, double searchRank,
                          String status, LocalDateTime publishedAt,
                          String neighborhood, Double latitude, Double longitude,
                          java.math.BigDecimal price, Integer sqft, String layout,
                          String availableFrom, String availableTo) {
}

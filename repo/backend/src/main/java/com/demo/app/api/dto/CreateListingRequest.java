package com.demo.app.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Discovery-critical listing payload. The geo, price, sqft, layout and
 * availability window fields back the search filters in
 * {@code ListingController.search} — without them the listing is invisible to
 * filtered queries even though the schema columns exist. Keep nulls allowed:
 * not every product is location- or date-bound.
 */
public record CreateListingRequest(
        @NotNull Long productId,
        @NotBlank String title,
        @NotBlank String slug,
        String summary,
        String[] tags,
        boolean featured,
        String neighborhood,
        Double latitude,
        Double longitude,
        BigDecimal price,
        Integer sqft,
        String layout,
        LocalDate availableFrom,
        LocalDate availableTo) {
}

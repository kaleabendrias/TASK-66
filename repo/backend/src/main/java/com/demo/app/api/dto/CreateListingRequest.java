package com.demo.app.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateListingRequest(
        @NotNull Long productId,
        @NotBlank String title,
        @NotBlank String slug,
        String summary,
        String[] tags,
        boolean featured) {
}

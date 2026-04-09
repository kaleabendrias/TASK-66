package com.demo.app.api.dto;

public record CreateListingRequest(Long productId, String title, String slug, String summary,
                                    String[] tags, boolean featured) {
}

package com.demo.app.domain.model;

import com.demo.app.domain.enums.ListingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Listing {
    private Long id;
    private Long productId;
    private String title;
    private String slug;
    private String summary;
    private List<String> tags;
    private boolean featured;
    private long viewCount;
    private long weeklyViews;
    private double searchRank;
    private String metadata;
    private ListingStatus status;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String neighborhood;
    private Double latitude;
    private Double longitude;
    private java.math.BigDecimal price;
    private Integer sqft;
    private String layout;
    private java.time.LocalDate availableFrom;
    private java.time.LocalDate availableTo;
}

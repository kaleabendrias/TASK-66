package com.demo.app.persistence.entity;

import com.demo.app.domain.enums.ListingStatus;
import com.demo.app.domain.model.Listing;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "listing")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "slug", nullable = false, length = 300)
    private String slug;

    @Column(name = "summary", length = 500)
    private String summary;

    @Column(name = "tags", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] tags;

    @Column(name = "featured", nullable = false)
    private boolean featured;

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    @Column(name = "search_rank", nullable = false)
    private double searchRank;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "neighborhood", length = 100)
    private String neighborhood;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "price", precision = 12, scale = 2)
    private java.math.BigDecimal price;

    @Column(name = "sqft")
    private Integer sqft;

    @Column(name = "layout", length = 50)
    private String layout;

    @Column(name = "available_from")
    private java.time.LocalDate availableFrom;

    @Column(name = "available_to")
    private java.time.LocalDate availableTo;

    public Listing toModel() {
        return Listing.builder()
                .id(id)
                .productId(productId)
                .title(title)
                .slug(slug)
                .summary(summary)
                .tags(tags != null ? Arrays.asList(tags) : List.of())
                .featured(featured)
                .viewCount(viewCount)
                .searchRank(searchRank)
                .metadata(metadata)
                .status(ListingStatus.valueOf(status))
                .publishedAt(publishedAt)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .neighborhood(neighborhood)
                .latitude(latitude)
                .longitude(longitude)
                .price(price)
                .sqft(sqft)
                .layout(layout)
                .availableFrom(availableFrom)
                .availableTo(availableTo)
                .build();
    }
}

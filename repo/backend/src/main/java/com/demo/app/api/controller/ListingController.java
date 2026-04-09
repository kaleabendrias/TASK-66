package com.demo.app.api.controller;

import com.demo.app.api.dto.CreateListingRequest;
import com.demo.app.api.dto.ListingDto;
import com.demo.app.application.service.ListingService;
import com.demo.app.application.service.ProductService;
import com.demo.app.domain.exception.OwnershipViolationException;
import com.demo.app.domain.model.Listing;
import com.demo.app.domain.model.Product;
import com.demo.app.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;
    private final ProductService productService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<ListingDto>> getPublished() {
        List<ListingDto> listings = listingService.getPublished().stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(listings);
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ListingDto> getBySlug(@PathVariable String slug) {
        Listing listing = listingService.getBySlug(slug);
        listingService.incrementViewCount(listing.getId());
        return ResponseEntity.ok(toDto(listing));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ListingDto>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String neighborhood,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radiusMiles,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate availableAfter,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate availableBefore,
            @RequestParam(required = false) java.math.BigDecimal minPrice,
            @RequestParam(required = false) java.math.BigDecimal maxPrice,
            @RequestParam(required = false) Integer minSqft,
            @RequestParam(required = false) Integer maxSqft,
            @RequestParam(required = false) String layout) {
        List<ListingDto> listings = listingService.searchAdvanced(q, neighborhood, lat, lng, radiusMiles,
                availableAfter, availableBefore, minPrice, maxPrice, minSqft, maxSqft, layout).stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(listings);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SELLER', 'ADMINISTRATOR')")
    public ResponseEntity<ListingDto> create(@RequestBody CreateListingRequest request) {
        Listing listing = Listing.builder()
                .productId(request.productId())
                .title(request.title())
                .slug(request.slug())
                .summary(request.summary())
                .tags(request.tags() != null ? Arrays.asList(request.tags()) : List.of())
                .featured(request.featured())
                .build();
        return ResponseEntity.ok(toDto(listingService.create(listing)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMINISTRATOR')")
    public ResponseEntity<ListingDto> update(@PathVariable Long id, @RequestBody CreateListingRequest request) {
        enforceListingOwnership(id);
        Listing listing = Listing.builder()
                .productId(request.productId())
                .title(request.title())
                .slug(request.slug())
                .summary(request.summary())
                .tags(request.tags() != null ? Arrays.asList(request.tags()) : List.of())
                .featured(request.featured())
                .build();
        return ResponseEntity.ok(toDto(listingService.update(id, listing)));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMINISTRATOR')")
    public ResponseEntity<ListingDto> publish(@PathVariable Long id) {
        enforceListingOwnership(id);
        return ResponseEntity.ok(toDto(listingService.publish(id)));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMINISTRATOR')")
    public ResponseEntity<ListingDto> archive(@PathVariable Long id) {
        enforceListingOwnership(id);
        return ResponseEntity.ok(toDto(listingService.archive(id)));
    }

    private void enforceListingOwnership(Long listingId) {
        if (isPrivileged()) return;
        Listing listing = listingService.getById(listingId);
        Product product = productService.getById(listing.getProductId());
        if (!product.getSellerId().equals(getCurrentUserId())) {
            throw new OwnershipViolationException("You can only modify your own listings");
        }
    }

    private boolean isPrivileged() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMINISTRATOR") ||
                a.getAuthority().equals("ROLE_MODERATOR"));
    }

    private Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }

    private ListingDto toDto(Listing l) {
        return new ListingDto(l.getId(), l.getProductId(), l.getTitle(), l.getSlug(), l.getSummary(),
                l.getTags() != null ? l.getTags().toArray(new String[0]) : null,
                l.isFeatured(), l.getViewCount(), l.getWeeklyViews(), l.getSearchRank(),
                l.getStatus() != null ? l.getStatus().name() : null, l.getPublishedAt(),
                l.getNeighborhood(), l.getLatitude(), l.getLongitude(),
                l.getPrice(), l.getSqft(), l.getLayout(),
                l.getAvailableFrom() != null ? l.getAvailableFrom().toString() : null,
                l.getAvailableTo() != null ? l.getAvailableTo().toString() : null);
    }
}

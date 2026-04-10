package com.demo.app.application.service;

import com.demo.app.domain.enums.ListingStatus;
import com.demo.app.domain.model.Listing;
import com.demo.app.persistence.entity.ListingEntity;
import com.demo.app.persistence.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ListingService {

    private final ListingRepository listingRepository;

    public List<Listing> getPublished() {
        return listingRepository.findPublishedByRank().stream()
                .map(ListingEntity::toModel)
                .toList();
    }

    public Listing getBySlug(String slug) {
        return listingRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Listing not found with slug: " + slug))
                .toModel();
    }

    public Listing getById(Long id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing not found: " + id))
                .toModel();
    }

    public Listing getByProduct(Long productId) {
        return listingRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Listing not found for product: " + productId))
                .toModel();
    }

    public Listing create(Listing listing) {
        if (listingRepository.findBySlug(listing.getSlug()).isPresent()) {
            throw new RuntimeException("Listing with slug '" + listing.getSlug() + "' already exists");
        }

        LocalDateTime now = LocalDateTime.now();
        ListingEntity entity = ListingEntity.builder()
                .productId(listing.getProductId())
                .title(listing.getTitle())
                .slug(listing.getSlug())
                .summary(listing.getSummary())
                .tags(listing.getTags() != null ? listing.getTags().toArray(new String[0]) : null)
                .featured(listing.isFeatured())
                .viewCount(0)
                .searchRank(0.0)
                .metadata(listing.getMetadata())
                .status(ListingStatus.DRAFT.name())
                .createdAt(now)
                .updatedAt(now)
                .build();
        return listingRepository.save(entity).toModel();
    }

    public Listing update(Long id, Listing listing) {
        ListingEntity entity = listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing not found: " + id));

        entity.setTitle(listing.getTitle());
        entity.setSlug(listing.getSlug());
        entity.setSummary(listing.getSummary());
        entity.setTags(listing.getTags() != null ? listing.getTags().toArray(new String[0]) : null);
        entity.setFeatured(listing.isFeatured());
        entity.setMetadata(listing.getMetadata());
        entity.setUpdatedAt(LocalDateTime.now());
        return listingRepository.save(entity).toModel();
    }

    public Listing publish(Long id) {
        ListingEntity entity = listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing not found: " + id));

        entity.setStatus(ListingStatus.PUBLISHED.name());
        entity.setPublishedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return listingRepository.save(entity).toModel();
    }

    public Listing archive(Long id) {
        ListingEntity entity = listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing not found: " + id));

        entity.setStatus(ListingStatus.ARCHIVED.name());
        entity.setUpdatedAt(LocalDateTime.now());
        return listingRepository.save(entity).toModel();
    }

    public void incrementViewCount(Long id) {
        ListingEntity entity = listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing not found: " + id));

        entity.setViewCount(entity.getViewCount() + 1);
        entity.setWeeklyViews(entity.getWeeklyViews() + 1);
        entity.setUpdatedAt(LocalDateTime.now());
        listingRepository.save(entity);
    }

    public List<Listing> searchBasic(String query) {
        String pattern = "%" + query + "%";
        return listingRepository.findByStatus(ListingStatus.PUBLISHED.name()).stream()
                .filter(e -> (e.getTitle() != null && e.getTitle().toLowerCase().contains(query.toLowerCase()))
                        || (e.getSummary() != null && e.getSummary().toLowerCase().contains(query.toLowerCase())))
                .map(ListingEntity::toModel)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Listing> searchAdvanced(String query, String neighborhood,
                                         Double userLat, Double userLng, Double radiusMiles,
                                         java.time.LocalDate availableAfter,
                                         java.time.LocalDate availableBefore,
                                         java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice,
                                         Integer minSqft, Integer maxSqft,
                                         String layout) {
        List<ListingEntity> all = listingRepository.findByStatus("PUBLISHED");

        return all.stream()
                .filter(l -> query == null || query.isBlank()
                        || l.getTitle().toLowerCase().contains(query.toLowerCase())
                        || (l.getSummary() != null && l.getSummary().toLowerCase().contains(query.toLowerCase())))
                .filter(l -> neighborhood == null || neighborhood.isBlank()
                        || (l.getNeighborhood() != null && l.getNeighborhood().equalsIgnoreCase(neighborhood)))
                .filter(l -> availableAfter == null
                        || (l.getAvailableFrom() != null && !l.getAvailableFrom().isAfter(availableAfter)))
                .filter(l -> availableBefore == null
                        || (l.getAvailableTo() != null && !l.getAvailableTo().isBefore(availableBefore)))
                .filter(l -> minPrice == null
                        || (l.getPrice() != null && l.getPrice().compareTo(minPrice) >= 0))
                .filter(l -> maxPrice == null
                        || (l.getPrice() != null && l.getPrice().compareTo(maxPrice) <= 0))
                .filter(l -> minSqft == null
                        || (l.getSqft() != null && l.getSqft() >= minSqft))
                .filter(l -> maxSqft == null
                        || (l.getSqft() != null && l.getSqft() <= maxSqft))
                .filter(l -> layout == null || layout.isBlank()
                        || (l.getLayout() != null && l.getLayout().equalsIgnoreCase(layout)))
                .filter(l -> {
                    if (radiusMiles == null || userLat == null || userLng == null) return true;
                    if (l.getLatitude() == null || l.getLongitude() == null) return false;
                    double distKm = haversine(userLat, userLng, l.getLatitude(), l.getLongitude());
                    double distMiles = distKm / 1.609344;
                    return distMiles <= radiusMiles;
                })
                .sorted((a, b) -> {
                    if (userLat != null && userLng != null
                            && a.getLatitude() != null && a.getLongitude() != null
                            && b.getLatitude() != null && b.getLongitude() != null) {
                        double distA = haversine(userLat, userLng, a.getLatitude(), a.getLongitude());
                        double distB = haversine(userLat, userLng, b.getLatitude(), b.getLongitude());
                        return Double.compare(distA, distB);
                    }
                    return Double.compare(b.getSearchRank(), a.getSearchRank());
                })
                .map(ListingEntity::toModel)
                .toList();
    }

    @Transactional
    public void refreshWeeklyViews() {
        // Decay weekly views by 15% daily to approximate a rolling 7-day window
        listingRepository.findAll().forEach(listing -> {
            long decayed = (long) (listing.getWeeklyViews() * 0.85);
            listing.setWeeklyViews(decayed);
            listing.setUpdatedAt(java.time.LocalDateTime.now());
            listingRepository.save(listing);
        });
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}

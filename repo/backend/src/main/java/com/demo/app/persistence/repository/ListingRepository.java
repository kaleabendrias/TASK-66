package com.demo.app.persistence.repository;

import com.demo.app.persistence.entity.ListingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ListingRepository extends JpaRepository<ListingEntity, Long> {
    Optional<ListingEntity> findByProductId(Long productId);
    Optional<ListingEntity> findBySlug(String slug);
    List<ListingEntity> findByStatus(String status);

    @Query("SELECT l FROM ListingEntity l WHERE l.status = 'PUBLISHED' ORDER BY l.searchRank DESC, l.publishedAt DESC")
    List<ListingEntity> findPublishedByRank();
}

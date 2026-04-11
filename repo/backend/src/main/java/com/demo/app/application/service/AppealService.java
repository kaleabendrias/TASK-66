package com.demo.app.application.service;

import com.demo.app.domain.exception.ResourceNotFoundException;
import com.demo.app.domain.model.Appeal;
import com.demo.app.persistence.entity.AppealEntity;
import com.demo.app.persistence.repository.AppealRepository;
import com.demo.app.persistence.repository.IncidentRepository;
import com.demo.app.persistence.repository.ListingRepository;
import com.demo.app.persistence.repository.OrderRepository;
import com.demo.app.persistence.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.demo.app.infrastructure.config.StatusTransitions;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class AppealService {

    private final AppealRepository appealRepository;
    private final IncidentRepository incidentRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ListingRepository listingRepository;

    public Appeal create(Long userId, String relatedEntityType, Long relatedEntityId, String reason) {
        if (relatedEntityType == null || relatedEntityType.isBlank()) {
            throw new IllegalArgumentException("relatedEntityType is required");
        }
        if (relatedEntityId == null) {
            throw new IllegalArgumentException("relatedEntityId is required");
        }

        // Strict DB-level existence check: the appeal must point at a row that
        // actually exists right now. Reject unknown entity types with the same
        // error surface so clients cannot probe for valid types via the delta
        // between 400s.
        String normalizedType = relatedEntityType.trim().toUpperCase(Locale.ROOT);
        JpaRepository<?, Long> repository = repositoryFor(normalizedType);
        if (repository == null) {
            throw new IllegalArgumentException(
                    "Unknown relatedEntityType: " + relatedEntityType +
                            " (expected one of INCIDENT, PRODUCT, ORDER, LISTING)");
        }
        if (!repository.existsById(relatedEntityId)) {
            throw new ResourceNotFoundException(normalizedType, relatedEntityId);
        }

        AppealEntity entity = AppealEntity.builder()
                .userId(userId)
                .relatedEntityType(normalizedType)
                .relatedEntityId(relatedEntityId)
                .reason(reason)
                .status("SUBMITTED")
                .createdAt(LocalDateTime.now())
                .build();

        return appealRepository.save(entity).toModel();
    }

    private JpaRepository<?, Long> repositoryFor(String type) {
        return switch (type) {
            case "INCIDENT" -> incidentRepository;
            case "PRODUCT" -> productRepository;
            case "ORDER" -> orderRepository;
            case "LISTING" -> listingRepository;
            default -> null;
        };
    }

    public Appeal review(Long appealId, Long reviewerId, String status, String reviewNotes) {
        AppealEntity entity = appealRepository.findById(appealId)
                .orElseThrow(() -> new ResourceNotFoundException("Appeal", appealId));

        StatusTransitions.validate(StatusTransitions.APPEAL, entity.getStatus(), status);

        LocalDateTime now = LocalDateTime.now();
        entity.setReviewerId(reviewerId);
        entity.setReviewNotes(reviewNotes);
        entity.setStatus(status);
        entity.setReviewedAt(now);

        if ("APPROVED".equals(status) || "REJECTED".equals(status)) {
            entity.setResolvedAt(now);
        }

        return appealRepository.save(entity).toModel();
    }

    @Transactional(readOnly = true)
    public Appeal getById(Long id) {
        return appealRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appeal", id))
                .toModel();
    }

    @Transactional(readOnly = true)
    public List<Appeal> getByUser(Long userId) {
        return appealRepository.findByUserId(userId).stream()
                .map(AppealEntity::toModel)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Appeal> getPending() {
        return appealRepository.findByStatus("SUBMITTED").stream()
                .map(AppealEntity::toModel)
                .toList();
    }
}

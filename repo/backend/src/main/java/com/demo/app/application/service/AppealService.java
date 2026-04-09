package com.demo.app.application.service;

import com.demo.app.domain.exception.ConflictException;
import com.demo.app.domain.exception.ResourceNotFoundException;
import com.demo.app.domain.model.Appeal;
import com.demo.app.persistence.entity.AppealEntity;
import com.demo.app.persistence.repository.AppealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AppealService {

    private final AppealRepository appealRepository;

    public Appeal create(Long userId, String relatedEntityType, Long relatedEntityId, String reason) {
        AppealEntity entity = AppealEntity.builder()
                .userId(userId)
                .relatedEntityType(relatedEntityType)
                .relatedEntityId(relatedEntityId)
                .reason(reason)
                .status("SUBMITTED")
                .createdAt(LocalDateTime.now())
                .build();

        return appealRepository.save(entity).toModel();
    }

    public Appeal review(Long appealId, Long reviewerId, String status, String reviewNotes) {
        AppealEntity entity = appealRepository.findById(appealId)
                .orElseThrow(() -> new ResourceNotFoundException("Appeal", appealId));

        if (!"SUBMITTED".equals(entity.getStatus()) && !"UNDER_REVIEW".equals(entity.getStatus())) {
            throw new ConflictException("Appeal is not in a reviewable state: " + entity.getStatus());
        }

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

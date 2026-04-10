package com.demo.app.application.service;

import com.demo.app.domain.model.BenefitItem;
import com.demo.app.domain.model.BenefitPackage;
import com.demo.app.persistence.entity.BenefitItemEntity;
import com.demo.app.persistence.entity.BenefitIssuanceLedgerEntity;
import com.demo.app.persistence.entity.BenefitRedemptionLedgerEntity;
import com.demo.app.domain.exception.ResourceNotFoundException;
import com.demo.app.persistence.repository.BenefitIssuanceLedgerRepository;
import com.demo.app.persistence.repository.BenefitItemRepository;
import com.demo.app.persistence.repository.BenefitPackageRepository;
import com.demo.app.persistence.repository.BenefitRedemptionLedgerRepository;
import com.demo.app.persistence.repository.OrderRepository;
import com.demo.app.persistence.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BenefitService {

    private final BenefitPackageRepository benefitPackageRepository;
    private final BenefitItemRepository benefitItemRepository;
    private final BenefitIssuanceLedgerRepository benefitIssuanceLedgerRepository;
    private final BenefitRedemptionLedgerRepository benefitRedemptionLedgerRepository;
    private final OrderRepository orderRepository;
    private final IncidentRepository incidentRepository;

    public List<BenefitPackage> getPackagesByTier(Long tierId) {
        return benefitPackageRepository.findByTierIdAndActiveTrue(tierId).stream()
                .map(e -> e.toModel())
                .toList();
    }

    public List<BenefitItem> getItemsByPackage(Long packageId) {
        return benefitItemRepository.findByPackageId(packageId).stream()
                .map(e -> e.toModel())
                .toList();
    }

    public BenefitIssuanceLedgerEntity issueBenefit(Long memberId, Long benefitItemId, Long issuedByUserId, String reference, String referenceType, Long referenceId) {
        if (referenceType == null || referenceType.isBlank()) {
            throw new IllegalArgumentException("referenceType is required. Must be ORDER or INCIDENT.");
        }

        Long orderId = null;
        Long incidentId = null;

        if ("ORDER".equals(referenceType)) {
                if (referenceId == null || !orderRepository.existsById(referenceId)) {
                    throw new ResourceNotFoundException("Order", referenceId);
                }
                orderId = referenceId;
            } else if ("INCIDENT".equals(referenceType)) {
                if (referenceId == null || !incidentRepository.existsById(referenceId)) {
                    throw new ResourceNotFoundException("Incident", referenceId);
                }
                incidentId = referenceId;
            } else {
            throw new IllegalArgumentException("Invalid referenceType: " + referenceType + ". Must be ORDER or INCIDENT.");
        }

        BenefitIssuanceLedgerEntity entity = BenefitIssuanceLedgerEntity.builder()
                .memberId(memberId)
                .benefitItemId(benefitItemId)
                .issuedBy(issuedByUserId)
                .reference(reference)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .orderId(orderId)
                .incidentId(incidentId)
                .issuedAt(LocalDateTime.now())
                .build();
        return benefitIssuanceLedgerRepository.save(entity);
    }

    public BenefitRedemptionLedgerEntity redeemBenefit(Long memberId, Long benefitItemId, String reference, Long orderCategoryId, Long orderSellerId, String referenceType, Long referenceId) {
        BenefitItemEntity item = benefitItemRepository.findById(benefitItemId)
                .orElseThrow(() -> new com.demo.app.domain.exception.ResourceNotFoundException("Benefit item", benefitItemId));

        // Check mutual exclusion: if this item has an exclusion group, verify no other
        // item from the same group has been redeemed with the same reference (transaction)
        if (item.getExclusionGroup() != null && reference != null) {
            List<BenefitRedemptionLedgerEntity> existing = benefitRedemptionLedgerRepository.findByMemberId(memberId);
            boolean conflict = existing.stream().anyMatch(r ->
                    reference.equals(r.getReference()) && benefitItemRepository.findById(r.getBenefitItemId())
                            .map(bi -> item.getExclusionGroup().equals(bi.getExclusionGroup()) && !bi.getId().equals(item.getId()))
                            .orElse(false));
            if (conflict) {
                throw new com.demo.app.domain.exception.ConflictException(
                        "Cannot redeem: another benefit in exclusion group '" + item.getExclusionGroup() + "' is already applied to this transaction");
            }
        }

        // Enforce date window
        LocalDateTime now = LocalDateTime.now();
        if (item.getValidFrom() != null && now.isBefore(item.getValidFrom())) {
            throw new com.demo.app.domain.exception.ConflictException("Benefit not yet active. Valid from: " + item.getValidFrom());
        }
        if (item.getValidTo() != null && now.isAfter(item.getValidTo())) {
            throw new com.demo.app.domain.exception.ConflictException("Benefit has expired. Valid until: " + item.getValidTo());
        }

        // Enforce category scope
        if (item.getCategoryId() != null && orderCategoryId != null
                && !item.getCategoryId().equals(orderCategoryId)) {
            throw new com.demo.app.domain.exception.ConflictException(
                    "Benefit is restricted to category " + item.getCategoryId() + " but order is category " + orderCategoryId);
        }

        // Enforce seller scope
        if (item.getSellerId() != null && orderSellerId != null
                && !item.getSellerId().equals(orderSellerId)) {
            throw new com.demo.app.domain.exception.ConflictException(
                    "Benefit is restricted to seller " + item.getSellerId() + " but order is from seller " + orderSellerId);
        }

        if (referenceType == null || referenceType.isBlank()) {
            throw new IllegalArgumentException("referenceType is required. Must be ORDER or INCIDENT.");
        }

        Long orderId = null;
        Long incidentId = null;

        if ("ORDER".equals(referenceType)) {
            if (referenceId == null || !orderRepository.existsById(referenceId)) {
                throw new ResourceNotFoundException("Order", referenceId);
            }
            orderId = referenceId;
        } else if ("INCIDENT".equals(referenceType)) {
            if (referenceId == null || !incidentRepository.existsById(referenceId)) {
                throw new ResourceNotFoundException("Incident", referenceId);
            }
            incidentId = referenceId;
        } else {
            throw new IllegalArgumentException("Invalid referenceType: " + referenceType + ". Must be ORDER or INCIDENT.");
        }

        BenefitRedemptionLedgerEntity entity = BenefitRedemptionLedgerEntity.builder()
                .memberId(memberId)
                .benefitItemId(benefitItemId)
                .reference(reference)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .orderId(orderId)
                .incidentId(incidentId)
                .redeemedAt(LocalDateTime.now())
                .build();
        return benefitRedemptionLedgerRepository.save(entity);
    }
}

package com.demo.app.application.service;

import com.demo.app.domain.model.BenefitItem;
import com.demo.app.domain.model.BenefitPackage;
import com.demo.app.persistence.entity.BenefitItemEntity;
import com.demo.app.persistence.entity.BenefitIssuanceLedgerEntity;
import com.demo.app.persistence.entity.BenefitRedemptionLedgerEntity;
import com.demo.app.persistence.repository.BenefitIssuanceLedgerRepository;
import com.demo.app.persistence.repository.BenefitItemRepository;
import com.demo.app.persistence.repository.BenefitPackageRepository;
import com.demo.app.persistence.repository.BenefitRedemptionLedgerRepository;
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

    public BenefitIssuanceLedgerEntity issueBenefit(Long memberId, Long benefitItemId, Long issuedByUserId, String reference) {
        BenefitIssuanceLedgerEntity entity = BenefitIssuanceLedgerEntity.builder()
                .memberId(memberId)
                .benefitItemId(benefitItemId)
                .issuedBy(issuedByUserId)
                .reference(reference)
                .issuedAt(LocalDateTime.now())
                .build();
        return benefitIssuanceLedgerRepository.save(entity);
    }

    public BenefitRedemptionLedgerEntity redeemBenefit(Long memberId, Long benefitItemId, String reference, Long orderCategoryId, Long orderSellerId) {
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

        BenefitRedemptionLedgerEntity entity = BenefitRedemptionLedgerEntity.builder()
                .memberId(memberId)
                .benefitItemId(benefitItemId)
                .reference(reference)
                .redeemedAt(LocalDateTime.now())
                .build();
        return benefitRedemptionLedgerRepository.save(entity);
    }
}

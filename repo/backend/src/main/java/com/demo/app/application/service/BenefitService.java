package com.demo.app.application.service;

import com.demo.app.domain.exception.OwnershipViolationException;
import com.demo.app.domain.model.BenefitItem;
import com.demo.app.domain.model.BenefitPackage;
import com.demo.app.persistence.entity.BenefitItemEntity;
import com.demo.app.persistence.entity.BenefitIssuanceLedgerEntity;
import com.demo.app.persistence.entity.BenefitRedemptionLedgerEntity;
import com.demo.app.persistence.entity.IncidentEntity;
import com.demo.app.persistence.entity.OrderEntity;
import com.demo.app.persistence.entity.ProductEntity;
import com.demo.app.domain.exception.ResourceNotFoundException;
import com.demo.app.persistence.repository.BenefitIssuanceLedgerRepository;
import com.demo.app.persistence.repository.BenefitItemRepository;
import com.demo.app.persistence.repository.BenefitPackageRepository;
import com.demo.app.persistence.repository.BenefitRedemptionLedgerRepository;
import com.demo.app.persistence.repository.OrderRepository;
import com.demo.app.persistence.repository.IncidentRepository;
import com.demo.app.persistence.repository.ProductRepository;
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
    private final ProductRepository productRepository;

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

    /**
     * Redeem a benefit on behalf of a member.
     *
     * <p>The caller no longer supplies categoryId / sellerId — those are
     * resolved directly from the persisted order or incident referenced by
     * {@code referenceType} / {@code referenceId}. This kills a class of
     * spoofing where a client could quote a permissive scope in the request
     * payload to bypass a restricted benefit.
     */
    public BenefitRedemptionLedgerEntity redeemBenefit(Long callerUserId, Long memberId, Long benefitItemId,
                                                       String reference, String referenceType, Long referenceId) {
        if (callerUserId == null) {
            throw new IllegalArgumentException("callerUserId is required for redemption (no anonymous redeem)");
        }
        if (referenceType == null || referenceType.isBlank()) {
            throw new IllegalArgumentException("referenceType is required. Must be ORDER or INCIDENT.");
        }
        if (referenceId == null) {
            throw new IllegalArgumentException("referenceId is required");
        }

        BenefitItemEntity item = benefitItemRepository.findById(benefitItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Benefit item", benefitItemId));

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

        // Resolve scope from the persisted entity. Any scope fields the client
        // sent on the request are ignored on purpose — see method javadoc.
        Long resolvedCategoryId;
        Long resolvedSellerId;
        Long orderId = null;
        Long incidentId = null;

        if ("ORDER".equals(referenceType)) {
            OrderEntity orderEntity = orderRepository.findById(referenceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order", referenceId));
            Long buyerId = orderEntity.getBuyer() != null ? orderEntity.getBuyer().getId() : orderEntity.getBuyerId();
            if (!callerUserId.equals(buyerId)) {
                throw new OwnershipViolationException(
                        "You can only redeem benefits against your own orders");
            }
            ProductEntity product = orderEntity.getProduct();
            if (product == null && orderEntity.getProductId() != null) {
                product = productRepository.findById(orderEntity.getProductId()).orElse(null);
            }
            resolvedCategoryId = product != null && product.getCategory() != null
                    ? product.getCategory().getId()
                    : null;
            resolvedSellerId = product != null && product.getSeller() != null
                    ? product.getSeller().getId()
                    : null;
            orderId = referenceId;
        } else if ("INCIDENT".equals(referenceType)) {
            IncidentEntity incidentEntity = incidentRepository.findById(referenceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Incident", referenceId));
            if (!callerUserId.equals(incidentEntity.getReporterId())) {
                throw new OwnershipViolationException(
                        "You can only redeem benefits against incidents you reported");
            }
            // Incidents don't carry a product/category, but they do carry the
            // seller they're filed against.
            resolvedCategoryId = null;
            resolvedSellerId = incidentEntity.getSellerId();
            incidentId = referenceId;
        } else {
            throw new IllegalArgumentException("Invalid referenceType: " + referenceType + ". Must be ORDER or INCIDENT.");
        }

        // Enforce category scope using the entity-resolved value.
        if (item.getCategoryId() != null
                && (resolvedCategoryId == null || !item.getCategoryId().equals(resolvedCategoryId))) {
            throw new com.demo.app.domain.exception.ConflictException(
                    "Benefit is restricted to category " + item.getCategoryId()
                            + " but the referenced " + referenceType.toLowerCase() + " resolves to category "
                            + resolvedCategoryId);
        }

        // Enforce seller scope using the entity-resolved value.
        if (item.getSellerId() != null
                && (resolvedSellerId == null || !item.getSellerId().equals(resolvedSellerId))) {
            throw new com.demo.app.domain.exception.ConflictException(
                    "Benefit is restricted to seller " + item.getSellerId()
                            + " but the referenced " + referenceType.toLowerCase() + " resolves to seller "
                            + resolvedSellerId);
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

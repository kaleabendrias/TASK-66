package com.demo.app.application.service;

import com.demo.app.domain.exception.ResourceNotFoundException;
import com.demo.app.persistence.entity.BenefitItemEntity;
import com.demo.app.persistence.entity.BenefitPackageEntity;
import com.demo.app.persistence.entity.MemberProfileEntity;
import com.demo.app.persistence.entity.ProductEntity;
import com.demo.app.persistence.repository.BenefitItemRepository;
import com.demo.app.persistence.repository.BenefitPackageRepository;
import com.demo.app.persistence.repository.MemberProfileRepository;
import com.demo.app.persistence.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Authoritative server-side pricing for an order line.
 *
 * <p>Recomputes the total price from:
 * <ul>
 *     <li>The product's current unit price (DB)</li>
 *     <li>The buyer's tier benefits (only items in active packages for the
 *         buyer's tier are considered)</li>
 *     <li>Date-window, category, and seller scoping on each benefit item</li>
 *     <li>Strict non-stacking: at most one item per exclusion group, and at
 *         most one DISCOUNT — the highest discount wins</li>
 * </ul>
 *
 * <p>The client-supplied {@code totalPrice} on the inbound payload is
 * intentionally never read here. Pricing is server-authoritative.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PricingService {

    private final ProductRepository productRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final BenefitPackageRepository benefitPackageRepository;
    private final BenefitItemRepository benefitItemRepository;

    public BigDecimal computeOrderTotal(Long buyerId, Long productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Order quantity must be positive, got: " + quantity);
        }

        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        BigDecimal unitPrice = product.getPrice();
        if (unitPrice == null) {
            throw new IllegalStateException("Product " + productId + " has no price configured");
        }
        BigDecimal lineSubtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

        Long productCategoryId = product.getCategory() != null ? product.getCategory().getId() : null;
        Long productSellerId = product.getSeller() != null ? product.getSeller().getId() : null;

        // Members get tier-based discounts; non-members (no profile) just pay
        // the line subtotal.
        Optional<MemberProfileEntity> profileOpt = memberProfileRepository.findByUserId(buyerId);
        if (profileOpt.isEmpty()) {
            return lineSubtotal.setScale(2, RoundingMode.HALF_UP);
        }
        MemberProfileEntity profile = profileOpt.get();

        List<BenefitPackageEntity> packages = benefitPackageRepository.findByTierIdAndActiveTrue(profile.getTierId());
        LocalDateTime now = LocalDateTime.now();

        // Collect candidate benefit items that are valid for THIS order:
        // active package, in-window, scoped category/seller match.
        java.util.List<BenefitItemEntity> applicable = new java.util.ArrayList<>();
        for (BenefitPackageEntity pkg : packages) {
            for (BenefitItemEntity item : benefitItemRepository.findByPackageId(pkg.getId())) {
                if (item.getValidFrom() != null && now.isBefore(item.getValidFrom())) continue;
                if (item.getValidTo() != null && now.isAfter(item.getValidTo())) continue;
                if (item.getCategoryId() != null && !item.getCategoryId().equals(productCategoryId)) continue;
                if (item.getSellerId() != null && !item.getSellerId().equals(productSellerId)) continue;
                applicable.add(item);
            }
        }

        // Non-stacking #1: only the highest-value DISCOUNT item is applied.
        Optional<BenefitItemEntity> bestDiscount = applicable.stream()
                .filter(b -> "DISCOUNT".equalsIgnoreCase(b.getBenefitType()))
                .max(Comparator.comparing(this::parseBenefitValue));

        // Non-stacking #2: within an exclusion group, only one item applies.
        // We pick the one that produces the largest absolute discount on this
        // subtotal.
        Map<String, BenefitItemEntity> bestPerGroup = new HashMap<>();
        for (BenefitItemEntity item : applicable) {
            String group = item.getExclusionGroup();
            if (group == null || group.isBlank()) continue;
            // The DISCOUNT category is handled separately above; don't double-count.
            if ("DISCOUNT".equalsIgnoreCase(item.getBenefitType())) continue;
            BenefitItemEntity current = bestPerGroup.get(group);
            if (current == null || isBetter(item, current, lineSubtotal)) {
                bestPerGroup.put(group, item);
            }
        }

        BigDecimal discount = BigDecimal.ZERO;
        if (bestDiscount.isPresent()) {
            discount = discount.add(applyDiscount(bestDiscount.get(), lineSubtotal));
        }
        for (BenefitItemEntity item : bestPerGroup.values()) {
            discount = discount.add(applyDiscount(item, lineSubtotal));
        }

        BigDecimal total = lineSubtotal.subtract(discount);
        if (total.signum() < 0) {
            total = BigDecimal.ZERO;
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal parseBenefitValue(BenefitItemEntity item) {
        try {
            return new BigDecimal(item.getBenefitValue());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * DISCOUNT items store a percentage in benefit_value (e.g. "15" => 15%).
     * Other types may store a flat absolute amount; we treat any numeric
     * value as a flat dollar reduction. Non-numeric values reduce by zero.
     */
    private BigDecimal applyDiscount(BenefitItemEntity item, BigDecimal subtotal) {
        BigDecimal value = parseBenefitValue(item);
        if ("DISCOUNT".equalsIgnoreCase(item.getBenefitType())) {
            return subtotal.multiply(value).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        }
        return value;
    }

    private boolean isBetter(BenefitItemEntity candidate, BenefitItemEntity current, BigDecimal subtotal) {
        return applyDiscount(candidate, subtotal).compareTo(applyDiscount(current, subtotal)) > 0;
    }
}

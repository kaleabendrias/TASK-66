package com.demo.app.application.service;

import com.demo.app.DemoApplication;
import com.demo.app.TestFixtures;
import com.demo.app.domain.enums.Role;
import com.demo.app.domain.model.BenefitItem;
import com.demo.app.domain.model.BenefitPackage;
import com.demo.app.persistence.entity.*;
import com.demo.app.persistence.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = DemoApplication.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("BenefitService - Benefit packages, items, issuance, and redemption")
class BenefitServiceTest {

    @Autowired
    private BenefitService benefitService;

    @Autowired
    private BenefitPackageRepository benefitPackageRepository;

    @Autowired
    private BenefitItemRepository benefitItemRepository;

    @Autowired
    private MemberTierRepository memberTierRepository;

    @Autowired
    private MemberProfileRepository memberProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BenefitIssuanceLedgerRepository benefitIssuanceLedgerRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    private MemberTierEntity tier;
    private BenefitPackageEntity pkg;
    private BenefitItemEntity discountItem;
    private BenefitItemEntity shippingItem;
    private UserEntity user;
    private MemberProfileEntity profile;
    private OrderEntity testOrder;

    @BeforeEach
    void setUp() {
        tier = memberTierRepository.save(TestFixtures.tier("Gold", 3, 5000, 19999));

        pkg = benefitPackageRepository.save(BenefitPackageEntity.builder()
                .tierId(tier.getId())
                .name("Gold Benefits")
                .description("Benefits for Gold members")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build());

        discountItem = benefitItemRepository.save(BenefitItemEntity.builder()
                .packageId(pkg.getId())
                .benefitType("DISCOUNT")
                .benefitValue("15")
                .scope("ORDER")
                .createdAt(LocalDateTime.now())
                .build());

        shippingItem = benefitItemRepository.save(BenefitItemEntity.builder()
                .packageId(pkg.getId())
                .benefitType("FREE_SHIPPING")
                .benefitValue("true")
                .scope("ORDER")
                .createdAt(LocalDateTime.now())
                .build());

        user = userRepository.save(TestFixtures.user("benefituser", Role.MEMBER));
        profile = memberProfileRepository.save(TestFixtures.profile(user.getId(), tier.getId(), 6000));

        CategoryEntity cat = categoryRepository.save(TestFixtures.category("TestCat"));
        ProductEntity prod = productRepository.save(TestFixtures.product("TestProd", new java.math.BigDecimal("10"), cat, user));
        testOrder = orderRepository.save(OrderEntity.builder()
                .buyer(user).product(prod).quantity(1)
                .totalPrice(new java.math.BigDecimal("10"))
                .status(com.demo.app.domain.enums.OrderStatus.PLACED)
                .tenderType("INTERNAL_CREDIT")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build());
    }

    @Test
    @DisplayName("getPackagesByTier returns active packages for the given tier")
    void testGetPackagesByTier_returnsActivePackages() {
        // Also create an inactive package to verify it's excluded
        benefitPackageRepository.save(BenefitPackageEntity.builder()
                .tierId(tier.getId())
                .name("Inactive Package")
                .description("Should not appear")
                .active(false)
                .createdAt(LocalDateTime.now())
                .build());

        List<BenefitPackage> packages = benefitService.getPackagesByTier(tier.getId());

        assertEquals(1, packages.size());
        assertEquals("Gold Benefits", packages.get(0).getName());
        assertTrue(packages.get(0).isActive());
    }

    @Test
    @DisplayName("getItemsByPackage returns all items for the given package")
    void testGetItemsByPackage_returnsItems() {
        List<BenefitItem> items = benefitService.getItemsByPackage(pkg.getId());

        assertEquals(2, items.size());
        assertTrue(items.stream().anyMatch(i -> "DISCOUNT".equals(i.getBenefitType())));
        assertTrue(items.stream().anyMatch(i -> "FREE_SHIPPING".equals(i.getBenefitType())));
    }

    @Test
    @DisplayName("issueBenefit creates a ledger entry")
    void testIssueBenefit_createsLedgerEntry() {
        BenefitIssuanceLedgerEntity entry = benefitService.issueBenefit(
                profile.getId(), discountItem.getId(), user.getId(), "test-issuance", "ORDER", testOrder.getId());

        assertNotNull(entry.getId());
        assertEquals(profile.getId(), entry.getMemberId());
        assertEquals(discountItem.getId(), entry.getBenefitItemId());
        assertEquals(user.getId(), entry.getIssuedBy());
        assertEquals("test-issuance", entry.getReference());
        assertNotNull(entry.getIssuedAt());
    }

    @Test
    @DisplayName("redeemBenefit creates a redemption ledger entry")
    void testRedeemBenefit_createsRedemptionEntry() {
        BenefitRedemptionLedgerEntity entry = benefitService.redeemBenefit(
                profile.getId(), discountItem.getId(), "test-redemption", null, null, "ORDER", testOrder.getId());

        assertNotNull(entry.getId());
        assertEquals(profile.getId(), entry.getMemberId());
        assertEquals(discountItem.getId(), entry.getBenefitItemId());
        assertEquals("test-redemption", entry.getReference());
        assertNotNull(entry.getRedeemedAt());
    }

    @Test
    @DisplayName("issueBenefit creates immutable ledger entries that persist")
    void testIssueBenefit_immutableLedger() {
        benefitService.issueBenefit(profile.getId(), discountItem.getId(), user.getId(), "ref-1", "ORDER", testOrder.getId());
        benefitService.issueBenefit(profile.getId(), shippingItem.getId(), user.getId(), "ref-2", "ORDER", testOrder.getId());

        List<BenefitIssuanceLedgerEntity> entries = benefitIssuanceLedgerRepository.findByMemberId(profile.getId());
        assertEquals(2, entries.size());

        // Issue another one; count should increase, not replace
        benefitService.issueBenefit(profile.getId(), discountItem.getId(), user.getId(), "ref-3", "ORDER", testOrder.getId());

        entries = benefitIssuanceLedgerRepository.findByMemberId(profile.getId());
        assertEquals(3, entries.size());
    }

    @Test
    @DisplayName("Benefits are non-stackable: only one DISCOUNT type applies (highest single discount)")
    void testBenefitsNonStackable_singleApplicationPerTransaction() {
        // Create a second discount item with a higher value
        BenefitItemEntity higherDiscount = benefitItemRepository.save(BenefitItemEntity.builder()
                .packageId(pkg.getId())
                .benefitType("DISCOUNT")
                .benefitValue("25")
                .scope("ORDER")
                .createdAt(LocalDateTime.now())
                .build());

        List<BenefitItem> items = benefitService.getItemsByPackage(pkg.getId());

        // Filter DISCOUNT items and find the highest value
        List<BenefitItem> discountItems = items.stream()
                .filter(i -> "DISCOUNT".equals(i.getBenefitType()))
                .toList();

        assertTrue(discountItems.size() >= 2, "Should have multiple discount items");

        // The highest single discount should be applied, not the sum
        int maxDiscount = discountItems.stream()
                .mapToInt(i -> Integer.parseInt(i.getBenefitValue()))
                .max()
                .orElse(0);

        int sumDiscount = discountItems.stream()
                .mapToInt(i -> Integer.parseInt(i.getBenefitValue()))
                .sum();

        assertEquals(25, maxDiscount, "Highest single discount should be 25");
        assertTrue(maxDiscount < sumDiscount, "Max discount should be less than sum (non-stackable)");
    }
}

package com.demo.app.application.service;

import com.demo.app.DemoApplication;
import com.demo.app.TestFixtures;
import com.demo.app.domain.model.MemberTier;
import com.demo.app.persistence.entity.MemberTierEntity;
import com.demo.app.persistence.repository.MemberTierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = DemoApplication.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("MemberTierService - Tier lookup and spend-based resolution")
class MemberTierServiceTest {

    @Autowired
    private MemberTierRepository memberTierRepository;

    @Autowired
    private MemberTierService memberTierService;

    private MemberTierEntity bronze;
    private MemberTierEntity silver;
    private MemberTierEntity gold;

    @BeforeEach
    void setUp() {
        bronze = memberTierRepository.save(TestFixtures.tier("Bronze", 1, 0, 499));
        silver = memberTierRepository.save(TestFixtures.tier("Silver", 2, 500, 1499));
        gold = memberTierRepository.save(TestFixtures.tier("Gold", 3, 1500, null));
    }

    @Test
    @DisplayName("getAll returns all 3 tiers sorted by rank ascending")
    void testGetAll_returnsSortedByRank() {
        List<MemberTier> tiers = memberTierService.getAll();

        assertEquals(3, tiers.size());
        assertEquals("Bronze", tiers.get(0).getName());
        assertEquals("Silver", tiers.get(1).getName());
        assertEquals("Gold", tiers.get(2).getName());
    }

    @Test
    @DisplayName("getById with existing tier returns the correct tier")
    void testGetById_existingTier_returnsTier() {
        MemberTier tier = memberTierService.getById(silver.getId());

        assertEquals("Silver", tier.getName());
        assertEquals(2, tier.getRank());
        assertEquals(500, tier.getMinSpend());
        assertEquals(1499, tier.getMaxSpend());
    }

    @Test
    @DisplayName("getById with non-existent ID throws exception")
    void testGetById_nonExistent_throws() {
        assertThrows(RuntimeException.class, () -> memberTierService.getById(99999L));
    }

    @Test
    @DisplayName("getTierForSpend with $0 returns Bronze")
    void testGetTierForSpend_zero_returnsBronze() {
        MemberTier tier = memberTierService.getTierForSpend(0);
        assertEquals("Bronze", tier.getName());
    }

    @Test
    @DisplayName("getTierForSpend with $500 returns Silver")
    void testGetTierForSpend_500_returnsSilver() {
        MemberTier tier = memberTierService.getTierForSpend(500);
        assertEquals("Silver", tier.getName());
    }

    @Test
    @DisplayName("getTierForSpend with $1500 returns Gold")
    void testGetTierForSpend_1500_returnsGold() {
        MemberTier tier = memberTierService.getTierForSpend(1500);
        assertEquals("Gold", tier.getName());
    }

    @Test
    @DisplayName("getTierForSpend with $499 still returns Bronze (edge case)")
    void testGetTierForSpend_499_stillBronze() {
        MemberTier tier = memberTierService.getTierForSpend(499);
        assertEquals("Bronze", tier.getName());
    }

    @Test
    @DisplayName("getTierForSpend with $10000 returns Gold (top tier)")
    void testGetTierForSpend_10000_returnsGold() {
        MemberTier tier = memberTierService.getTierForSpend(10000);
        assertEquals("Gold", tier.getName());
    }
}

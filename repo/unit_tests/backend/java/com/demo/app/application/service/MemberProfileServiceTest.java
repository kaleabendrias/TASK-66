package com.demo.app.application.service;

import com.demo.app.DemoApplication;
import com.demo.app.TestFixtures;
import com.demo.app.domain.enums.Role;
import com.demo.app.domain.model.MemberProfile;
import com.demo.app.persistence.entity.MemberProfileEntity;
import com.demo.app.persistence.entity.MemberTierEntity;
import com.demo.app.persistence.entity.PointsLedgerEntity; // entity class name unchanged, table renamed to spend_ledger
import com.demo.app.persistence.entity.UserEntity;
import com.demo.app.persistence.repository.MemberProfileRepository;
import com.demo.app.persistence.repository.MemberTierRepository;
import com.demo.app.persistence.repository.PointsLedgerRepository;
import com.demo.app.persistence.repository.UserRepository;
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
@DisplayName("MemberProfileService - Profile management, spend, and tier upgrades")
class MemberProfileServiceTest {

    @Autowired
    private MemberProfileService memberProfileService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MemberTierRepository memberTierRepository;

    @Autowired
    private MemberProfileRepository memberProfileRepository;

    @Autowired
    private PointsLedgerRepository pointsLedgerRepository;

    private MemberTierEntity bronze;
    private MemberTierEntity silver;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        bronze = memberTierRepository.save(TestFixtures.tier("Bronze", 1, 0, 499));
        silver = memberTierRepository.save(TestFixtures.tier("Silver", 2, 500, 1499));
        user = userRepository.save(TestFixtures.user("profileuser", Role.MEMBER));
    }

    @Test
    @DisplayName("getByUserId with existing profile returns the profile")
    void testGetByUserId_existingProfile_returnsProfile() {
        memberProfileRepository.save(TestFixtures.profile(user.getId(), bronze.getId(), 100));

        MemberProfile profile = memberProfileService.getByUserId(user.getId());

        assertNotNull(profile);
        assertEquals(user.getId(), profile.getUserId());
        assertEquals(100, profile.getTotalSpend());
    }

    @Test
    @DisplayName("getByUserId with no profile throws RuntimeException")
    void testGetByUserId_noProfile_throws() {
        assertThrows(RuntimeException.class, () -> memberProfileService.getByUserId(user.getId()));
    }

    @Test
    @DisplayName("createProfile sets default Bronze tier")
    void testCreateProfile_setsDefaultBronzeTier() {
        MemberProfile profile = memberProfileService.createProfile(user.getId(), null);

        assertNotNull(profile);
        assertEquals(bronze.getId(), profile.getTierId());
        assertEquals(0, profile.getTotalSpend());
    }

    @Test
    @DisplayName("createProfile encrypts the phone number")
    void testCreateProfile_encryptsPhone() {
        memberProfileService.createProfile(user.getId(), "+15551234567");

        MemberProfileEntity entity = memberProfileRepository.findByUserId(user.getId()).orElseThrow();
        assertNotNull(entity.getPhoneEncrypted());
        assertNotEquals("+15551234567", entity.getPhoneEncrypted());
    }

    @Test
    @DisplayName("createProfile masks the phone number correctly")
    void testCreateProfile_masksPhone() {
        MemberProfile profile = memberProfileService.createProfile(user.getId(), "+15551234567");

        assertEquals("***-***-4567", profile.getPhoneMasked());
    }

    @Test
    @DisplayName("addSpend updates the spend balance")
    void testAddPoints_updatesBalance() {
        memberProfileService.createProfile(user.getId(), null);

        MemberProfile updated = memberProfileService.addSpend(user.getId(), 500, "purchase-reward");

        assertEquals(500, updated.getTotalSpend());
    }

    @Test
    @DisplayName("addSpend creates a ledger entry")
    void testAddPoints_createsLedgerEntry() {
        MemberProfile profile = memberProfileService.createProfile(user.getId(), null);

        memberProfileService.addSpend(user.getId(), 500, "purchase-reward");

        MemberProfileEntity entity = memberProfileRepository.findByUserId(user.getId()).orElseThrow();
        List<PointsLedgerEntity> ledger = pointsLedgerRepository.findByMemberIdOrderByCreatedAtDesc(entity.getId());

        assertFalse(ledger.isEmpty());
        assertEquals(500, ledger.get(0).getAmount());
        assertEquals(500, ledger.get(0).getSpendAfter());
        assertEquals("EARNED", ledger.get(0).getEntryType());
        assertEquals("purchase-reward", ledger.get(0).getReference());
    }

    @Test
    @DisplayName("addSpend triggers tier upgrade from Bronze to Silver at $500 spend")
    void testAddSpend_triggersTierUpgrade() {
        memberProfileService.createProfile(user.getId(), null);

        MemberProfile updated = memberProfileService.addSpend(user.getId(), 500, "big-purchase");

        assertEquals(silver.getId(), updated.getTierId());
    }

    @Test
    @DisplayName("deductSpend subtracts from the balance")
    void testDeductPoints_subtractsBalance() {
        memberProfileService.createProfile(user.getId(), null);
        memberProfileService.addSpend(user.getId(), 500, "earn");

        MemberProfile updated = memberProfileService.deductSpend(user.getId(), 200, "redeem");

        assertEquals(300, updated.getTotalSpend());
    }

    @Test
    @DisplayName("deductSpend with insufficient balance throws RuntimeException")
    void testDeductPoints_insufficientBalance_throws() {
        memberProfileService.createProfile(user.getId(), null);
        memberProfileService.addSpend(user.getId(), 100, "earn");

        assertThrows(RuntimeException.class,
                () -> memberProfileService.deductSpend(user.getId(), 200, "over-redeem"));
    }
}

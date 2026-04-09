package com.demo.app.application.service;

import com.demo.app.domain.model.MemberProfile;
import com.demo.app.domain.model.MemberTier;
import com.demo.app.infrastructure.encryption.FieldEncryptor;
import com.demo.app.infrastructure.encryption.PhoneMaskUtil;
import com.demo.app.persistence.entity.MemberProfileEntity;
import com.demo.app.persistence.entity.PointsLedgerEntity;
import com.demo.app.persistence.repository.MemberProfileRepository;
import com.demo.app.persistence.repository.PointsLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberProfileService {

    private final MemberProfileRepository memberProfileRepository;
    private final MemberTierService memberTierService;
    private final PointsLedgerRepository pointsLedgerRepository;
    private final FieldEncryptor fieldEncryptor;

    public MemberProfile getByUserId(Long userId) {
        return memberProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Member profile not found for user: " + userId))
                .toModel();
    }

    public MemberProfile createProfile(Long userId, String phone) {
        MemberTier bronzeTier = memberTierService.getAll().stream()
                .filter(t -> t.getRank() == 1)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Bronze tier (rank 1) not found"));

        MemberProfileEntity entity = MemberProfileEntity.builder()
                .userId(userId)
                .tierId(bronzeTier.getId())
                .totalSpend(0)
                .joinedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        if (phone != null && !phone.isBlank()) {
            entity.setPhoneEncrypted(fieldEncryptor.encrypt(phone));
            entity.setPhoneMasked(PhoneMaskUtil.mask(phone));
        }

        return memberProfileRepository.save(entity).toModel();
    }

    public MemberProfile updatePhone(Long userId, String phone) {
        MemberProfileEntity entity = memberProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Member profile not found for user: " + userId));

        entity.setPhoneEncrypted(fieldEncryptor.encrypt(phone));
        entity.setPhoneMasked(PhoneMaskUtil.mask(phone));
        entity.setUpdatedAt(LocalDateTime.now());

        return memberProfileRepository.save(entity).toModel();
    }

    public MemberProfile addSpend(Long userId, int amount, String reference) {
        MemberProfileEntity entity = memberProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Member profile not found for user: " + userId));

        int newBalance = entity.getTotalSpend() + amount;
        entity.setTotalSpend(newBalance);
        entity.setUpdatedAt(LocalDateTime.now());

        PointsLedgerEntity ledgerEntry = PointsLedgerEntity.builder()
                .memberId(entity.getId())
                .amount(amount)
                .spendAfter(newBalance)
                .entryType("EARNED")
                .reference(reference)
                .createdAt(LocalDateTime.now())
                .build();
        pointsLedgerRepository.save(ledgerEntry);

        MemberTier newTier = memberTierService.getTierForSpend(newBalance);
        if (!newTier.getId().equals(entity.getTierId())) {
            entity.setTierId(newTier.getId());
        }

        return memberProfileRepository.save(entity).toModel();
    }

    public MemberProfile deductSpend(Long userId, int amount, String reference) {
        MemberProfileEntity entity = memberProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Member profile not found for user: " + userId));

        if (entity.getTotalSpend() < amount) {
            throw new RuntimeException("Insufficient spend balance. Current: " + entity.getTotalSpend() + ", requested: " + amount);
        }

        int newBalance = entity.getTotalSpend() - amount;
        entity.setTotalSpend(newBalance);
        entity.setUpdatedAt(LocalDateTime.now());

        PointsLedgerEntity ledgerEntry = PointsLedgerEntity.builder()
                .memberId(entity.getId())
                .amount(-amount)
                .spendAfter(newBalance)
                .entryType("REDEEMED")
                .reference(reference)
                .createdAt(LocalDateTime.now())
                .build();
        pointsLedgerRepository.save(ledgerEntry);

        MemberTier newTier = memberTierService.getTierForSpend(newBalance);
        if (!newTier.getId().equals(entity.getTierId())) {
            entity.setTierId(newTier.getId());
        }

        return memberProfileRepository.save(entity).toModel();
    }

    public List<PointsLedgerEntity> getSpendHistory(Long userId) {
        MemberProfileEntity entity = memberProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Member profile not found for user: " + userId));
        return pointsLedgerRepository.findByMemberIdOrderByCreatedAtDesc(entity.getId());
    }
}

package com.demo.app.api.controller;

import com.demo.app.api.dto.MemberProfileDto;
import com.demo.app.api.dto.PointsAdjustmentRequest;
import com.demo.app.api.dto.PointsLedgerEntryDto;
import com.demo.app.api.dto.UpdatePhoneRequest;
import com.demo.app.application.service.MemberProfileService;
import com.demo.app.application.service.MemberTierService;
import com.demo.app.domain.model.MemberProfile;
import com.demo.app.domain.model.MemberTier;
import com.demo.app.persistence.entity.PointsLedgerEntity;
import com.demo.app.persistence.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberProfileController {

    private final MemberProfileService memberProfileService;
    private final MemberTierService memberTierService;
    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<MemberProfileDto> getMyProfile() {
        Long userId = getCurrentUserId();
        MemberProfile profile = memberProfileService.getByUserId(userId);
        MemberTier tier = memberTierService.getById(profile.getTierId());
        return ResponseEntity.ok(toDto(profile, tier.getName()));
    }

    @PutMapping("/me/phone")
    public ResponseEntity<MemberProfileDto> updatePhone(@Valid @RequestBody UpdatePhoneRequest request) {
        Long userId = getCurrentUserId();
        MemberProfile profile = memberProfileService.updatePhone(userId, request.phone());
        MemberTier tier = memberTierService.getById(profile.getTierId());
        return ResponseEntity.ok(toDto(profile, tier.getName()));
    }

    @PostMapping("/me/spend")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMINISTRATOR')")
    public ResponseEntity<MemberProfileDto> adjustSpend(@Valid @RequestBody PointsAdjustmentRequest request) {
        Long userId = getCurrentUserId();
        MemberProfile profile;
        if (request.amount() >= 0) {
            profile = memberProfileService.addSpend(userId, request.amount(), request.reference());
        } else {
            profile = memberProfileService.deductSpend(userId, -request.amount(), request.reference());
        }
        MemberTier tier = memberTierService.getById(profile.getTierId());
        return ResponseEntity.ok(toDto(profile, tier.getName()));
    }

    @GetMapping("/me/spend/history")
    public ResponseEntity<List<PointsLedgerEntryDto>> getSpendHistory() {
        Long userId = getCurrentUserId();
        List<PointsLedgerEntryDto> history = memberProfileService.getSpendHistory(userId).stream()
                .map(this::toLedgerDto)
                .toList();
        return ResponseEntity.ok(history);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMINISTRATOR')")
    public ResponseEntity<MemberProfileDto> getProfileByUserId(@PathVariable Long userId) {
        MemberProfile profile = memberProfileService.getByUserId(userId);
        MemberTier tier = memberTierService.getById(profile.getTierId());
        return ResponseEntity.ok(toDto(profile, tier.getName()));
    }

    private Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username))
                .getId();
    }

    private MemberProfileDto toDto(MemberProfile profile, String tierName) {
        return new MemberProfileDto(
                profile.getId(),
                profile.getUserId(),
                profile.getTierId(),
                tierName,
                profile.getTotalSpend(),
                profile.getPhoneMasked(),
                profile.getJoinedAt()
        );
    }

    private PointsLedgerEntryDto toLedgerDto(PointsLedgerEntity entry) {
        return new PointsLedgerEntryDto(
                entry.getId(),
                entry.getAmount(),
                entry.getSpendAfter(),
                entry.getEntryType(),
                entry.getReference(),
                entry.getCreatedAt()
        );
    }
}

package com.demo.app.api.controller;

import com.demo.app.api.dto.BenefitItemDto;
import com.demo.app.api.dto.BenefitPackageDto;
import com.demo.app.api.dto.IssueBenefitRequest;
import com.demo.app.api.dto.RedeemBenefitRequest;
import com.demo.app.application.service.BenefitService;
import com.demo.app.application.service.MemberProfileService;
import com.demo.app.domain.model.BenefitItem;
import com.demo.app.domain.model.BenefitPackage;
import com.demo.app.domain.model.MemberProfile;
import com.demo.app.persistence.entity.BenefitIssuanceLedgerEntity;
import com.demo.app.persistence.entity.BenefitRedemptionLedgerEntity;
import com.demo.app.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/benefits")
@RequiredArgsConstructor
public class BenefitController {

    private final BenefitService benefitService;
    private final MemberProfileService memberProfileService;
    private final UserRepository userRepository;

    @GetMapping("/packages/tier/{tierId}")
    public ResponseEntity<List<BenefitPackageDto>> getPackagesByTier(@PathVariable Long tierId) {
        List<BenefitPackageDto> packages = benefitService.getPackagesByTier(tierId).stream()
                .map(this::toPackageDto)
                .toList();
        return ResponseEntity.ok(packages);
    }

    @GetMapping("/items/package/{packageId}")
    public ResponseEntity<List<BenefitItemDto>> getItemsByPackage(@PathVariable Long packageId) {
        List<BenefitItemDto> items = benefitService.getItemsByPackage(packageId).stream()
                .map(this::toItemDto)
                .toList();
        return ResponseEntity.ok(items);
    }

    @PostMapping("/issue")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMINISTRATOR')")
    public ResponseEntity<Void> issueBenefit(@RequestBody IssueBenefitRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Long issuedByUserId = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username))
                .getId();

        benefitService.issueBenefit(request.memberId(), request.benefitItemId(), issuedByUserId, request.reference());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/redeem")
    public ResponseEntity<Void> redeemBenefit(@RequestBody RedeemBenefitRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Long userId = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username))
                .getId();

        MemberProfile profile = memberProfileService.getByUserId(userId);
        benefitService.redeemBenefit(profile.getId(), request.benefitItemId(), request.reference());
        return ResponseEntity.ok().build();
    }

    private BenefitPackageDto toPackageDto(BenefitPackage pkg) {
        return new BenefitPackageDto(
                pkg.getId(),
                pkg.getTierId(),
                pkg.getName(),
                pkg.getDescription(),
                pkg.isActive()
        );
    }

    private BenefitItemDto toItemDto(BenefitItem item) {
        return new BenefitItemDto(
                item.getId(),
                item.getPackageId(),
                item.getBenefitType(),
                item.getBenefitValue(),
                item.getScope(),
                item.getExclusionGroup()
        );
    }
}

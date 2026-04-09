package com.demo.app.api.controller;

import com.demo.app.api.dto.MemberTierDto;
import com.demo.app.application.service.MemberTierService;
import com.demo.app.domain.model.MemberTier;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tiers")
@RequiredArgsConstructor
public class MemberTierController {

    private final MemberTierService memberTierService;

    @GetMapping
    public ResponseEntity<List<MemberTierDto>> getAll() {
        List<MemberTierDto> tiers = memberTierService.getAll().stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(tiers);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MemberTierDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(toDto(memberTierService.getById(id)));
    }

    private MemberTierDto toDto(MemberTier tier) {
        return new MemberTierDto(
                tier.getId(),
                tier.getName(),
                tier.getRank(),
                tier.getMinSpend(),
                tier.getMaxSpend()
        );
    }
}

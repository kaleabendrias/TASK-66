package com.demo.app.application.service;

import com.demo.app.domain.exception.ResourceNotFoundException;
import com.demo.app.domain.model.MemberTier;
import com.demo.app.persistence.repository.MemberTierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberTierService {

    private final MemberTierRepository memberTierRepository;

    public List<MemberTier> getAll() {
        return memberTierRepository.findAll(Sort.by("rank")).stream()
                .map(e -> e.toModel())
                .toList();
    }

    public MemberTier getById(Long id) {
        return memberTierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tier", id))
                .toModel();
    }

    public MemberTier getTierForSpend(int totalSpend) {
        return memberTierRepository.findAll(Sort.by(Sort.Direction.DESC, "rank")).stream()
                .filter(tier -> totalSpend >= tier.getMinSpend()
                        && (tier.getMaxSpend() == null || totalSpend <= tier.getMaxSpend()))
                .map(e -> e.toModel())
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Tier for spend", totalSpend));
    }
}

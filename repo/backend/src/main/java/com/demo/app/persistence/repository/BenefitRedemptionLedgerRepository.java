package com.demo.app.persistence.repository;

import com.demo.app.persistence.entity.BenefitRedemptionLedgerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BenefitRedemptionLedgerRepository extends JpaRepository<BenefitRedemptionLedgerEntity, Long> {
    List<BenefitRedemptionLedgerEntity> findByMemberId(Long memberId);
}

package com.demo.app.persistence.repository;

import com.demo.app.persistence.entity.BenefitIssuanceLedgerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BenefitIssuanceLedgerRepository extends JpaRepository<BenefitIssuanceLedgerEntity, Long> {
    List<BenefitIssuanceLedgerEntity> findByMemberId(Long memberId);
}

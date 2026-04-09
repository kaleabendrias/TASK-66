package com.demo.app.persistence.repository;

import com.demo.app.persistence.entity.PointsLedgerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointsLedgerRepository extends JpaRepository<PointsLedgerEntity, Long> {
    List<PointsLedgerEntity> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}

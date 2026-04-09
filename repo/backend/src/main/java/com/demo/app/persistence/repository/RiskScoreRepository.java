package com.demo.app.persistence.repository;

import com.demo.app.persistence.entity.RiskScoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RiskScoreRepository extends JpaRepository<RiskScoreEntity, Long> {

    Optional<RiskScoreEntity> findByUserId(Long userId);

    List<RiskScoreEntity> findByScoreGreaterThanOrderByScoreDesc(double threshold);
}

package com.demo.app.persistence.repository;

import com.demo.app.persistence.entity.BenefitPackageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BenefitPackageRepository extends JpaRepository<BenefitPackageEntity, Long> {
    List<BenefitPackageEntity> findByTierId(Long tierId);
    List<BenefitPackageEntity> findByTierIdAndActiveTrue(Long tierId);
}

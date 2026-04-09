package com.demo.app.persistence.repository;

import com.demo.app.persistence.entity.BenefitItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BenefitItemRepository extends JpaRepository<BenefitItemEntity, Long> {
    List<BenefitItemEntity> findByPackageId(Long packageId);
}

package com.demo.app.persistence.repository;

import com.demo.app.domain.enums.ProductStatus;
import com.demo.app.persistence.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
    List<ProductEntity> findBySeller_Id(Long sellerId);
    List<ProductEntity> findByStatus(ProductStatus status);
}

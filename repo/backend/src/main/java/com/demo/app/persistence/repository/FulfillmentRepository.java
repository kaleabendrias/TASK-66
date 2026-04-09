package com.demo.app.persistence.repository;

import com.demo.app.persistence.entity.FulfillmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FulfillmentRepository extends JpaRepository<FulfillmentEntity, Long> {
    Optional<FulfillmentEntity> findByOrderId(Long orderId);
    List<FulfillmentEntity> findByStatus(String status);
    Optional<FulfillmentEntity> findByIdempotencyKey(String idempotencyKey);
}

package com.demo.app.persistence.repository;

import com.demo.app.persistence.entity.FulfillmentStepEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FulfillmentStepRepository extends JpaRepository<FulfillmentStepEntity, Long> {
    List<FulfillmentStepEntity> findByFulfillmentIdOrderByCreatedAtAsc(Long fulfillmentId);
}

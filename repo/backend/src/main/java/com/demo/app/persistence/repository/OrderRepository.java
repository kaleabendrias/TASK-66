package com.demo.app.persistence.repository;

import com.demo.app.persistence.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    List<OrderEntity> findByBuyer_Id(Long buyerId);
}

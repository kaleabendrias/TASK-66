package com.demo.app.persistence.repository;

import com.demo.app.persistence.entity.WarehouseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WarehouseRepository extends JpaRepository<WarehouseEntity, Long> {
    Optional<WarehouseEntity> findByCode(String code);
    List<WarehouseEntity> findByActiveTrue();
}

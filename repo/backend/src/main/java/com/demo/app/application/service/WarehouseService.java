package com.demo.app.application.service;

import com.demo.app.domain.model.Warehouse;
import com.demo.app.persistence.entity.WarehouseEntity;
import com.demo.app.persistence.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;

    public List<Warehouse> getAll() {
        return warehouseRepository.findAll().stream()
                .map(WarehouseEntity::toModel)
                .toList();
    }

    public List<Warehouse> getActive() {
        return warehouseRepository.findByActiveTrue().stream()
                .map(WarehouseEntity::toModel)
                .toList();
    }

    public Warehouse getById(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Warehouse not found: " + id))
                .toModel();
    }

    public Warehouse getByCode(String code) {
        return warehouseRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Warehouse not found with code: " + code))
                .toModel();
    }

    public Warehouse create(Warehouse warehouse) {
        WarehouseEntity entity = WarehouseEntity.builder()
                .name(warehouse.getName())
                .code(warehouse.getCode())
                .location(warehouse.getLocation())
                .active(warehouse.isActive())
                .createdAt(LocalDateTime.now())
                .build();
        return warehouseRepository.save(entity).toModel();
    }

    public Warehouse update(Long id, Warehouse warehouse) {
        WarehouseEntity entity = warehouseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Warehouse not found: " + id));
        entity.setName(warehouse.getName());
        entity.setCode(warehouse.getCode());
        entity.setLocation(warehouse.getLocation());
        entity.setActive(warehouse.isActive());
        return warehouseRepository.save(entity).toModel();
    }
}

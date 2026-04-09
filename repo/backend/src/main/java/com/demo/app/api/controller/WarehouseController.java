package com.demo.app.api.controller;

import com.demo.app.api.dto.WarehouseDto;
import com.demo.app.application.service.WarehouseService;
import com.demo.app.domain.model.Warehouse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping
    public ResponseEntity<List<WarehouseDto>> getActive() {
        List<WarehouseDto> warehouses = warehouseService.getActive().stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(warehouses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WarehouseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(toDto(warehouseService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<WarehouseDto> create(@RequestBody WarehouseDto dto) {
        Warehouse warehouse = Warehouse.builder()
                .name(dto.name())
                .code(dto.code())
                .location(dto.location())
                .active(dto.active())
                .build();
        return ResponseEntity.ok(toDto(warehouseService.create(warehouse)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<WarehouseDto> update(@PathVariable Long id, @RequestBody WarehouseDto dto) {
        Warehouse warehouse = Warehouse.builder()
                .name(dto.name())
                .code(dto.code())
                .location(dto.location())
                .active(dto.active())
                .build();
        return ResponseEntity.ok(toDto(warehouseService.update(id, warehouse)));
    }

    private WarehouseDto toDto(Warehouse w) {
        return new WarehouseDto(w.getId(), w.getName(), w.getCode(), w.getLocation(), w.isActive());
    }
}

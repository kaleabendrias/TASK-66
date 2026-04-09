package com.demo.app.api.controller;

import com.demo.app.api.dto.InventoryItemDto;
import com.demo.app.application.service.InventoryService;
import com.demo.app.domain.model.InventoryItem;
import com.demo.app.persistence.entity.UserEntity;
import com.demo.app.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final UserRepository userRepository;

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<InventoryItemDto>> getByProduct(@PathVariable Long productId) {
        List<InventoryItemDto> items = inventoryService.getByProduct(productId).stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(items);
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<InventoryItemDto>> getLowStock() {
        List<InventoryItemDto> items = inventoryService.getLowStockItems().stream()
                .map(i -> new InventoryItemDto(i.getId(), i.getProductId(), i.getWarehouseId(), null,
                        i.getQuantityOnHand(), i.getQuantityReserved(), i.getQuantityAvailable(),
                        i.getLowStockThreshold(), true))
                .toList();
        return ResponseEntity.ok(items);
    }

    @PostMapping("/adjust")
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'ADMINISTRATOR')")
    public ResponseEntity<InventoryItemDto> adjustStock(@RequestBody Map<String, Object> body) {
        Long inventoryItemId = ((Number) body.get("inventoryItemId")).longValue();
        int quantityChange = ((Number) body.get("quantityChange")).intValue();
        String movementType = (String) body.get("movementType");
        String referenceDocument = (String) body.get("referenceDocument");
        String notes = (String) body.get("notes");

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        InventoryItem item = inventoryService.adjustStock(inventoryItemId, quantityChange, movementType,
                referenceDocument, user.getId(), notes);
        return ResponseEntity.ok(toDto(item));
    }

    private InventoryItemDto toDto(InventoryItem i) {
        boolean lowStock = (i.getQuantityOnHand() - i.getQuantityReserved()) < i.getLowStockThreshold();
        return new InventoryItemDto(i.getId(), i.getProductId(), i.getWarehouseId(), null,
                i.getQuantityOnHand(), i.getQuantityReserved(), i.getQuantityAvailable(),
                i.getLowStockThreshold(), lowStock);
    }
}

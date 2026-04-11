package com.demo.app.api.controller;

import com.demo.app.api.dto.InboundRequest;
import com.demo.app.api.dto.InventoryItemDto;
import com.demo.app.api.dto.OutboundRequest;
import com.demo.app.api.dto.StocktakeRequest;
import com.demo.app.application.service.InventoryService;
import com.demo.app.application.service.ProductService;
import com.demo.app.domain.exception.OwnershipViolationException;
import com.demo.app.domain.model.InventoryItem;
import com.demo.app.domain.model.Product;
import com.demo.app.infrastructure.audit.Audited;
import com.demo.app.persistence.repository.UserRepository;
import com.demo.app.persistence.repository.WarehouseRepository;
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
    private final ProductService productService;
    private final UserRepository userRepository;
    private final WarehouseRepository warehouseRepository;

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<InventoryItemDto>> getByProduct(@PathVariable Long productId) {
        // Seller scoping: sellers can only see inventory for their own products
        if (isSeller()) {
            Long currentUserId = getCurrentUserId();
            Product product = productService.getById(productId);
            if (!product.getSellerId().equals(currentUserId)) {
                throw new OwnershipViolationException(
                        "Sellers can only view inventory for their own products");
            }
        }
        List<InventoryItemDto> items = inventoryService.getByProduct(productId).stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(items);
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<InventoryItemDto>> getLowStock() {
        List<InventoryItemDto> items = inventoryService.getLowStockItems().stream()
                .map(this::toDto)
                .toList();
        // Seller scoping: only show low-stock for seller's own products
        if (isSeller()) {
            Long currentUserId = getCurrentUserId();
            items = items.stream().filter(i -> {
                try {
                    Product p = productService.getById(i.productId());
                    return p.getSellerId().equals(currentUserId);
                } catch (Exception e) { return false; }
            }).toList();
        }
        return ResponseEntity.ok(items);
    }

    @PostMapping("/adjust")
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'SELLER', 'ADMINISTRATOR')")
    public ResponseEntity<InventoryItemDto> adjustStock(@RequestBody Map<String, Object> body) {
        Long inventoryItemId = ((Number) body.get("inventoryItemId")).longValue();
        int quantityChange = ((Number) body.get("quantityChange")).intValue();
        String movementType = (String) body.get("movementType");
        String referenceDocument = (String) body.get("referenceDocument");
        String notes = (String) body.get("notes");

        enforceSellerScope(inventoryItemId);

        Long operatorId = getCurrentUserId();

        InventoryItem item = inventoryService.adjustStock(inventoryItemId, quantityChange, movementType,
                referenceDocument, operatorId, notes);
        return ResponseEntity.ok(toDto(item));
    }

    @PostMapping("/inbound")
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'SELLER', 'ADMINISTRATOR')")
    @Audited(entityType = "INVENTORY", action = "INBOUND")
    public ResponseEntity<InventoryItemDto> recordInbound(@RequestBody @jakarta.validation.Valid InboundRequest request) {
        enforceSellerScope(request.inventoryItemId());
        Long operatorId = getCurrentUserId();
        InventoryItem item = inventoryService.adjustStock(
                request.inventoryItemId(), request.quantity(), "INBOUND",
                request.referenceDocument(), operatorId, request.notes());
        return ResponseEntity.ok(toDto(item));
    }

    @PostMapping("/outbound")
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'SELLER', 'ADMINISTRATOR')")
    @Audited(entityType = "INVENTORY", action = "OUTBOUND")
    public ResponseEntity<InventoryItemDto> recordOutbound(@RequestBody @jakarta.validation.Valid OutboundRequest request) {
        enforceSellerScope(request.inventoryItemId());
        Long operatorId = getCurrentUserId();
        InventoryItem item = inventoryService.adjustStock(
                request.inventoryItemId(), -request.quantity(), "OUTBOUND",
                request.referenceDocument(), operatorId, request.notes());
        return ResponseEntity.ok(toDto(item));
    }

    @PostMapping("/stocktake")
    @PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'SELLER', 'ADMINISTRATOR')")
    @Audited(entityType = "INVENTORY", action = "STOCKTAKE")
    public ResponseEntity<InventoryItemDto> recordStocktake(@RequestBody @jakarta.validation.Valid StocktakeRequest request) {
        // Stocktake addresses a (productId, warehouseId) pair, so scope by product directly.
        if (isSeller()) {
            Product product = productService.getById(request.productId());
            if (!product.getSellerId().equals(getCurrentUserId())) {
                throw new OwnershipViolationException(
                        "Sellers can only stocktake their own products");
            }
        }
        Long operatorId = getCurrentUserId();
        InventoryItem current = inventoryService.getByProductAndWarehouse(request.productId(), request.warehouseId());
        int delta = request.countedQuantity() - current.getQuantityOnHand();
        InventoryItem item = inventoryService.adjustStock(
                current.getId(), delta, "STOCKTAKE",
                request.referenceDocument(), operatorId, "Stocktake: counted=" + request.countedQuantity());
        return ResponseEntity.ok(toDto(item));
    }

    /**
     * Object-level enforcement: sellers may only mutate inventory rows whose
     * underlying product is their own. Admins and warehouse staff bypass this
     * check (their role @PreAuthorize already gates the call).
     */
    private void enforceSellerScope(Long inventoryItemId) {
        if (!isSeller()) {
            return;
        }
        InventoryItem item = inventoryService.getById(inventoryItemId);
        Product product = productService.getById(item.getProductId());
        if (product.getSellerId() == null || !product.getSellerId().equals(getCurrentUserId())) {
            throw new OwnershipViolationException(
                    "Sellers can only modify inventory for their own products");
        }
    }

    private boolean isSeller() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SELLER"))
                && auth.getAuthorities().stream().noneMatch(a ->
                        a.getAuthority().equals("ROLE_WAREHOUSE_STAFF") || a.getAuthority().equals("ROLE_ADMINISTRATOR"));
    }

    private Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }

    private InventoryItemDto toDto(InventoryItem i) {
        int available = i.getQuantityOnHand() - i.getQuantityReserved();
        int threshold = Math.max(5, i.getLowStockThreshold());
        boolean lowStock = available < threshold;
        String whName = warehouseRepository.findById(i.getWarehouseId())
                .map(w -> w.getName()).orElse(null);
        return new InventoryItemDto(i.getId(), i.getProductId(), i.getWarehouseId(), whName,
                i.getQuantityOnHand(), i.getQuantityReserved(), i.getQuantityAvailable(),
                i.getLowStockThreshold(), lowStock);
    }
}

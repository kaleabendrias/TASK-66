package com.demo.app.api.controller;

import com.demo.app.api.dto.ProductDto;
import com.demo.app.api.mapper.ProductMapper;
import com.demo.app.application.service.ProductService;
import com.demo.app.domain.enums.ProductStatus;
import com.demo.app.domain.exception.OwnershipViolationException;
import com.demo.app.domain.model.Product;
import com.demo.app.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductMapper productMapper;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<ProductDto>> getAll() {
        List<ProductDto> products = productService.getAll().stream()
                .map(productMapper::toDto)
                .toList();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getById(@PathVariable Long id) {
        Product product = productService.getById(id);
        return ResponseEntity.ok(productMapper.toDto(product));
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<List<ProductDto>> getBySeller(@PathVariable Long sellerId) {
        List<ProductDto> products = productService.getBySeller(sellerId).stream()
                .map(productMapper::toDto)
                .toList();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<ProductDto>> getByStatus(@PathVariable ProductStatus status) {
        List<ProductDto> products = productService.getByStatus(status).stream()
                .map(productMapper::toDto)
                .toList();
        return ResponseEntity.ok(products);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SELLER', 'ADMINISTRATOR')")
    public ResponseEntity<ProductDto> create(@RequestBody ProductDto dto) {
        Product model = productMapper.toModel(dto);
        // Force sellerId to authenticated user, ignoring any value from the DTO
        model.setSellerId(getCurrentUserId());
        Product product = productService.create(model);
        return ResponseEntity.ok(productMapper.toDto(product));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMINISTRATOR')")
    public ResponseEntity<ProductDto> update(@PathVariable Long id, @RequestBody ProductDto dto) {
        Product existing = productService.getById(id);
        if (!existing.getSellerId().equals(getCurrentUserId()) && !isAdmin()) {
            throw new OwnershipViolationException("You can only update your own products");
        }
        Product model = productMapper.toModel(dto);
        // Preserve original sellerId — don't allow changing it
        model.setSellerId(existing.getSellerId());
        Product product = productService.update(id, model);
        return ResponseEntity.ok(productMapper.toDto(product));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMINISTRATOR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Product existing = productService.getById(id);
        if (!existing.getSellerId().equals(getCurrentUserId()) && !isAdmin()) {
            throw new OwnershipViolationException("You can only delete your own products");
        }
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }

    private boolean isAdmin() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRATOR"));
    }
}

package com.demo.app.application.service;

import com.demo.app.domain.enums.ProductStatus;
import com.demo.app.domain.model.Product;
import com.demo.app.persistence.entity.CategoryEntity;
import com.demo.app.persistence.entity.ProductEntity;
import com.demo.app.persistence.entity.UserEntity;
import com.demo.app.persistence.repository.CategoryRepository;
import com.demo.app.persistence.repository.ProductRepository;
import com.demo.app.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<Product> getAll() {
        return productRepository.findAll().stream()
                .map(ProductEntity::toModel)
                .toList();
    }

    @Transactional(readOnly = true)
    public Product getById(Long id) {
        return productRepository.findById(id)
                .map(ProductEntity::toModel)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Product> getBySeller(Long sellerId) {
        return productRepository.findBySeller_Id(sellerId).stream()
                .map(ProductEntity::toModel)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Product> getByStatus(ProductStatus status) {
        return productRepository.findByStatus(status).stream()
                .map(ProductEntity::toModel)
                .toList();
    }

    @Transactional
    public Product create(Product product) {
        CategoryEntity category = product.getCategoryId() != null
                ? categoryRepository.findById(product.getCategoryId()).orElse(null)
                : null;
        UserEntity seller = product.getSellerId() != null
                ? userRepository.findById(product.getSellerId()).orElse(null)
                : null;

        ProductEntity entity = ProductEntity.builder()
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .category(category)
                .seller(seller)
                .status(product.getStatus() != null ? product.getStatus() : ProductStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return productRepository.save(entity).toModel();
    }

    @Transactional
    public Product update(Long id, Product product) {
        ProductEntity entity = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        entity.setName(product.getName());
        entity.setDescription(product.getDescription());
        entity.setPrice(product.getPrice());
        entity.setStockQuantity(product.getStockQuantity());
        entity.setStatus(product.getStatus());
        entity.setUpdatedAt(LocalDateTime.now());

        if (product.getCategoryId() != null) {
            entity.setCategory(categoryRepository.findById(product.getCategoryId()).orElse(null));
        }
        if (product.getSellerId() != null) {
            entity.setSeller(userRepository.findById(product.getSellerId()).orElse(null));
        }

        return productRepository.save(entity).toModel();
    }

    @Transactional
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }
}

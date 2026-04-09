package com.demo.app.api.mapper;

import com.demo.app.api.dto.ProductDto;
import com.demo.app.application.service.CategoryService;
import com.demo.app.application.service.UserService;
import com.demo.app.domain.enums.ProductStatus;
import com.demo.app.domain.model.Category;
import com.demo.app.domain.model.Product;
import com.demo.app.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductMapper {

    private final CategoryService categoryService;
    private final UserService userService;

    public ProductDto toDto(Product product) {
        String categoryName = null;
        if (product.getCategoryId() != null) {
            try {
                Category category = categoryService.getById(product.getCategoryId());
                categoryName = category.getName();
            } catch (Exception ignored) {}
        }

        String sellerName = null;
        if (product.getSellerId() != null) {
            try {
                User seller = userService.getById(product.getSellerId());
                sellerName = seller.getDisplayName();
            } catch (Exception ignored) {}
        }

        return new ProductDto(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getCategoryId(),
                categoryName,
                product.getSellerId(),
                sellerName,
                product.getStatus() != null ? product.getStatus().name() : null
        );
    }

    public Product toModel(ProductDto dto) {
        return Product.builder()
                .id(dto.id())
                .name(dto.name())
                .description(dto.description())
                .price(dto.price())
                .stockQuantity(dto.stockQuantity())
                .categoryId(dto.categoryId())
                .sellerId(dto.sellerId())
                .status(dto.status() != null ? ProductStatus.valueOf(dto.status()) : null)
                .build();
    }
}

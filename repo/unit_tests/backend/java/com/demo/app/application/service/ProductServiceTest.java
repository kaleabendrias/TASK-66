package com.demo.app.application.service;

import com.demo.app.DemoApplication;
import com.demo.app.TestFixtures;
import com.demo.app.domain.enums.ProductStatus;
import com.demo.app.domain.enums.Role;
import com.demo.app.domain.model.Product;
import com.demo.app.persistence.entity.CategoryEntity;
import com.demo.app.persistence.entity.UserEntity;
import com.demo.app.persistence.repository.CategoryRepository;
import com.demo.app.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = DemoApplication.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("ProductService - CRUD operations")
class ProductServiceTest {

    @Autowired private ProductService productService;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private UserRepository userRepository;

    private CategoryEntity category;
    private UserEntity seller;

    @BeforeEach
    void setUp() {
        category = categoryRepository.save(TestFixtures.category("Gadgets"));
        seller = userRepository.save(TestFixtures.user("prodsvc_seller", Role.SELLER));
    }

    @Test
    @DisplayName("create saves product")
    void create_savesProduct() {
        Product input = Product.builder()
                .name("Phone").description("Smart phone").price(BigDecimal.valueOf(599))
                .stockQuantity(50).categoryId(category.getId()).sellerId(seller.getId())
                .status(ProductStatus.APPROVED).build();
        Product result = productService.create(input);
        assertNotNull(result.getId());
        assertEquals("Phone", result.getName());
    }

    @Test
    @DisplayName("getAll returns products")
    void getAll_returnsProducts() {
        productService.create(Product.builder()
                .name("A").description("d").price(BigDecimal.ONE)
                .stockQuantity(1).categoryId(category.getId()).sellerId(seller.getId())
                .status(ProductStatus.APPROVED).build());
        List<Product> all = productService.getAll();
        assertFalse(all.isEmpty());
    }

    @Test
    @DisplayName("update modifies product")
    void update_modifiesProduct() {
        Product created = productService.create(Product.builder()
                .name("Old").description("d").price(BigDecimal.ONE)
                .stockQuantity(1).categoryId(category.getId()).sellerId(seller.getId())
                .status(ProductStatus.APPROVED).build());
        Product updated = productService.update(created.getId(), Product.builder()
                .name("New").description("updated").price(BigDecimal.TEN)
                .stockQuantity(5).status(ProductStatus.APPROVED).build());
        assertEquals("New", updated.getName());
        assertEquals(BigDecimal.TEN, updated.getPrice());
    }

    @Test
    @DisplayName("delete removes product")
    void delete_removesProduct() {
        Product created = productService.create(Product.builder()
                .name("ToDelete").description("d").price(BigDecimal.ONE)
                .stockQuantity(1).categoryId(category.getId()).sellerId(seller.getId())
                .status(ProductStatus.APPROVED).build());
        productService.delete(created.getId());
        assertThrows(RuntimeException.class, () -> productService.getById(created.getId()));
    }

    @Test
    @DisplayName("getById non-existent throws")
    void getById_nonExistent_throws() {
        assertThrows(RuntimeException.class, () -> productService.getById(99999L));
    }
}

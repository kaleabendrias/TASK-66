package com.demo.app.application.service;

import com.demo.app.DemoApplication;
import com.demo.app.TestFixtures;
import com.demo.app.domain.model.Category;
import com.demo.app.persistence.repository.CategoryRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = DemoApplication.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("CategoryService")
class CategoryServiceTest {
    @Autowired private CategoryService categoryService;
    @Autowired private CategoryRepository categoryRepository;

    @BeforeEach void setUp() {
        categoryRepository.save(TestFixtures.category("CatSvc1"));
    }

    @Test void testGetAll() { assertFalse(categoryService.getAll().isEmpty()); }
    @Test void testGetById() {
        Long id = categoryRepository.findAll().get(0).getId();
        assertNotNull(categoryService.getById(id));
    }
}

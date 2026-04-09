package com.demo.app.application.service;

import com.demo.app.domain.model.Category;
import com.demo.app.persistence.entity.CategoryEntity;
import com.demo.app.persistence.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<Category> getAll() {
        return categoryRepository.findAll().stream()
                .map(CategoryEntity::toModel)
                .toList();
    }

    @Transactional(readOnly = true)
    public Category getById(Long id) {
        return categoryRepository.findById(id)
                .map(CategoryEntity::toModel)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
    }

    @Transactional
    public Category create(Category category) {
        CategoryEntity entity = CategoryEntity.builder()
                .name(category.getName())
                .description(category.getDescription())
                .createdAt(LocalDateTime.now())
                .build();
        return categoryRepository.save(entity).toModel();
    }

    @Transactional
    public Category update(Long id, Category category) {
        CategoryEntity entity = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
        entity.setName(category.getName());
        entity.setDescription(category.getDescription());
        return categoryRepository.save(entity).toModel();
    }

    @Transactional
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new RuntimeException("Category not found with id: " + id);
        }
        categoryRepository.deleteById(id);
    }
}

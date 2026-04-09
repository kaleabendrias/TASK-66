package com.demo.app.api.controller;

import com.demo.app.api.dto.CategoryDto;
import com.demo.app.application.service.CategoryService;
import com.demo.app.domain.model.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryDto>> getAll() {
        List<CategoryDto> categories = categoryService.getAll().stream()
                .map(c -> new CategoryDto(c.getId(), c.getName(), c.getDescription()))
                .toList();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryDto> getById(@PathVariable Long id) {
        Category c = categoryService.getById(id);
        return ResponseEntity.ok(new CategoryDto(c.getId(), c.getName(), c.getDescription()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<CategoryDto> create(@RequestBody CategoryDto dto) {
        Category category = categoryService.create(
                Category.builder().name(dto.name()).description(dto.description()).build());
        return ResponseEntity.ok(new CategoryDto(category.getId(), category.getName(), category.getDescription()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<CategoryDto> update(@PathVariable Long id, @RequestBody CategoryDto dto) {
        Category category = categoryService.update(id,
                Category.builder().name(dto.name()).description(dto.description()).build());
        return ResponseEntity.ok(new CategoryDto(category.getId(), category.getName(), category.getDescription()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

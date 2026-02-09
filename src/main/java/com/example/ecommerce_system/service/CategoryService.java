package com.example.ecommerce_system.service;

import com.example.ecommerce_system.dto.category.CategoryRequestDto;
import com.example.ecommerce_system.dto.category.CategoryResponseDto;
import com.example.ecommerce_system.exception.category.CategoryNotFoundException;
import com.example.ecommerce_system.exception.category.DuplicateCategoryException;
import com.example.ecommerce_system.model.Category;
import com.example.ecommerce_system.repository.CategoryRepository;
import com.example.ecommerce_system.store.CategoryStore;
import com.example.ecommerce_system.util.mapper.CategoryMapper;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@AllArgsConstructor
@Service
public class CategoryService {

    private final CategoryMapper mapper;
    private final CategoryRepository categoryRepository;

    /**
     * Create a new category with the provided name and description.
     * Validates that no category with the same name already exists before creation.
     */
    public CategoryResponseDto createCategory(CategoryRequestDto request) {
        Optional<Category> existing = categoryRepository.findCategoryByName(request.getName());
        if (existing.isPresent()) throw new DuplicateCategoryException(request.getName());
        Category category = new Category(
                UUID.randomUUID(),
                request.getName(),
                request.getDescription(),
                Instant.now(),
                Instant.now()
        );
        Category saved = categoryRepository.save(category);
        return mapper.toDTO(saved);
    }

    private CategoryResponseDto map(Category category) {
        return CategoryResponseDto.builder()
                .categoryId(category.getCategoryId())
                .name(category.getName())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    /**
     * Update the category identified by the given ID with new values.
     * Validates that the category exists and the new name doesn't conflict with existing categories.
     */
    public CategoryResponseDto updateCategory(UUID id, CategoryRequestDto request) {
        Category existingOption = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id.toString()));

        boolean isDuplicate = categoryRepository.findCategoryByName(request.getName()).isPresent();
        if (isDuplicate) throw new DuplicateCategoryException(request.getName());

        Category updated = new Category(
                existingOption.getCategoryId(),
                request.getName() == null ? existingOption.getName() : request.getName(),
                request.getDescription() == null ? existingOption.getDescription() : request.getDescription(),
                existingOption.getCreatedAt(),
                Instant.now()
        );
        Category saved = categoryRepository.save(updated);
        return mapper.toDTO(saved);
    }

    public CategoryResponseDto getCategory(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id.toString()));
        return mapper.toDTO(category);
    }

    public CategoryResponseDto getCategory(String name) {
        Category category = categoryRepository.findCategoryByName(name)
                .orElseThrow(() -> new CategoryNotFoundException(name));
        return mapper.toDTO(category);
    }

    /**
     * Search for a category with name or description containing query.
     */
    public List<CategoryResponseDto> getCategories(String query, int limit, int offset) {
        Category probe = new Category();
        probe.setName(query);
        probe.setDescription(query);

        ExampleMatcher matcher = ExampleMatcher.matching()
                .withIgnoreNullValues()
                .withIgnoreCase()
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING);

        List<Category> categories = categoryRepository.findAll(
                Example.of(probe, matcher),
                PageRequest.of(offset, limit)
        ).getContent();
        return mapper.toDTOList(categories);
    }

    public List<CategoryResponseDto> getAllCategories(int limit, int offset) {
        List<Category> categories = categoryRepository.findAll(PageRequest.of(offset, limit)).getContent();
        return mapper.toDTOList(categories);
    }

    /**
     * Delete a category by ID.
     * Validates that the category exists before deletion.
     */
    public void deleteCategory(UUID id) {
        categoryRepository.findById(id).orElseThrow(() -> new CategoryNotFoundException(id.toString()));
        categoryRepository.deleteById(id);
    }
}

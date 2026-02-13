package com.example.ecommerce_system.service;

import com.example.ecommerce_system.dto.category.CategoryFilter;
import com.example.ecommerce_system.dto.category.CategoryRequestDto;
import com.example.ecommerce_system.dto.category.CategoryResponseDto;
import com.example.ecommerce_system.exception.category.CategoryNotFoundException;
import com.example.ecommerce_system.exception.category.DuplicateCategoryException;
import com.example.ecommerce_system.model.Category;
import com.example.ecommerce_system.repository.CategoryRepository;
import com.example.ecommerce_system.util.mapper.CategoryMapper;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @CacheEvict(value = {"categories", "paginated"}, allEntries = true)
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

    /**
     * Update the category identified by the given ID with new values.
     * Validates that the category exists and the new name doesn't conflict with existing categories.
     */
    @CacheEvict(value = {"categories", "paginated"}, allEntries = true)
    @Transactional
    public CategoryResponseDto updateCategory(UUID id, CategoryRequestDto request) {
        Category existingOption = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id.toString()));

        boolean isDuplicate = categoryRepository.findCategoryByName(request.getName()).isPresent();
        if (isDuplicate) throw new DuplicateCategoryException(request.getName());

        existingOption.setUpdatedAt(Instant.now());
        if(request.getName() != null)
            existingOption.setName(request.getName());
        if(request.getDescription() != null)
            existingOption.setDescription(request.getDescription());

        return mapper.toDTO(existingOption);
    }

    @Cacheable(value = "categories", key = "#id")
    public CategoryResponseDto getCategory(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id.toString()));
        return mapper.toDTO(category);
    }

    @Cacheable(value = "categories", key = "'name_' + #name")
    public CategoryResponseDto getCategory(String name) {
        Category category = categoryRepository.findCategoryByName(name)
                .orElseThrow(() -> new CategoryNotFoundException(name));
        return mapper.toDTO(category);
    }

    /**
     * Search for a category with name or description containing query.
     */
    @Cacheable(value = "paginated", key = "'search_categories_' + #filter.toString() + '_' + #limit + '_' + #offset")
    public List<CategoryResponseDto> getCategories(CategoryFilter filter, int limit, int offset) {
        Category probe = Category.builder()
                .name(filter.getName())
                .description(filter.getDescription())
                .build();

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

    @Cacheable(value = "paginated", key = "'all_categories_' + #limit + '_' + #offset")
    public List<CategoryResponseDto> getAllCategories(int limit, int offset) {
        List<Category> categories = categoryRepository.findAll(PageRequest.of(offset, limit)).getContent();
        return mapper.toDTOList(categories);
    }

    /**
     * Delete a category by ID.
     * Validates that the category exists before deletion.
     */
    @CacheEvict(value = {"categories", "paginated"}, allEntries = true)
    public void deleteCategory(UUID id) {
        categoryRepository.findById(id).orElseThrow(() -> new CategoryNotFoundException(id.toString()));
        categoryRepository.deleteById(id);
    }
}

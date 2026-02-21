package com.example.ecommerce_system;

import com.example.ecommerce_system.dto.category.CategoryFilter;
import com.example.ecommerce_system.dto.category.CategoryRequestDto;
import com.example.ecommerce_system.dto.category.CategoryResponseDto;
import com.example.ecommerce_system.exception.category.CategoryNotFoundException;
import com.example.ecommerce_system.exception.category.DuplicateCategoryException;
import com.example.ecommerce_system.exception.category.CategoryDeletionException;
import com.example.ecommerce_system.model.Category;
import com.example.ecommerce_system.repository.CategoryRepository;
import com.example.ecommerce_system.service.CategoryService;
import com.example.ecommerce_system.util.mapper.CategoryMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper mapper;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    @DisplayName("Should create category successfully")
    void shouldCreateCategorySuccessfully() {
        CategoryRequestDto request = new CategoryRequestDto("Electronics", "Electronic items");
        Category savedCategory = new Category(
                UUID.randomUUID(), "Electronics", "Electronic items",
                Instant.now(), Instant.now()
        );
        CategoryResponseDto responseDto = CategoryResponseDto.builder()
                .categoryId(savedCategory.getCategoryId())
                .name("Electronics")
                .description("Electronic items")
                .build();

        when(categoryRepository.findCategoryByName("Electronics")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);
        when(mapper.toDTO(savedCategory)).thenReturn(responseDto);

        CategoryResponseDto response = categoryService.createCategory(request);

        Assertions.assertEquals("Electronics", response.getName());
        verify(categoryRepository).findCategoryByName("Electronics");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("Should throw error when creating a category with duplicate name")
    void shouldThrowWhenCreatingDuplicateCategory() {
        CategoryRequestDto request = new CategoryRequestDto("Electronics", "Electronic items");
        Category existingCategory = new Category(
                UUID.randomUUID(), "Electronics", "Electronic items",
                Instant.now(), Instant.now()
        );

        when(categoryRepository.findCategoryByName("Electronics")).thenReturn(Optional.of(existingCategory));

        Assertions.assertThrows(
                DuplicateCategoryException.class,
                () -> categoryService.createCategory(request)
        );

        verify(categoryRepository).findCategoryByName("Electronics");
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update category successfully")
    void shouldUpdateCategorySuccessfully() {
        UUID categoryId = UUID.randomUUID();
        CategoryRequestDto request = new CategoryRequestDto("Updated Name", "Updated Description");
        Category existingCategory = new Category(
                categoryId, "Old Name", "Old Description",
                Instant.now(), Instant.now()
        );
        CategoryResponseDto responseDto = CategoryResponseDto.builder()
                .categoryId(categoryId)
                .name("Updated Name")
                .description("Updated Description")
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.findCategoryByName("Updated Name")).thenReturn(Optional.empty());
        when(mapper.toDTO(existingCategory)).thenReturn(responseDto);

        CategoryResponseDto response = categoryService.updateCategory(categoryId, request);

        Assertions.assertEquals("Updated Name", response.getName());
        Assertions.assertEquals("Updated Description", response.getDescription());
        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository).findCategoryByName("Updated Name");
    }

    @Test
    @DisplayName("Should throw error when updating non-existing category")
    void shouldThrowWhenUpdatingMissingCategory() {
        UUID id = UUID.randomUUID();
        CategoryRequestDto request = new CategoryRequestDto("new", "desc");

        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        Assertions.assertThrows(
                CategoryNotFoundException.class,
                () -> categoryService.updateCategory(id, request)
        );

        verify(categoryRepository).findById(id);
    }

    @Test
    @DisplayName("Should throw error when updating to duplicate name")
    void shouldThrowWhenUpdatingToDuplicateName() {
        UUID categoryId = UUID.randomUUID();
        CategoryRequestDto request = new CategoryRequestDto("Existing Name", "Description");
        Category existingCategory = new Category(
                categoryId, "Old Name", "Old Description",
                Instant.now(), Instant.now()
        );
        Category duplicateCategory = new Category(
                UUID.randomUUID(), "Existing Name", "Some description",
                Instant.now(), Instant.now()
        );

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.findCategoryByName("Existing Name")).thenReturn(Optional.of(duplicateCategory));

        Assertions.assertThrows(
                DuplicateCategoryException.class,
                () -> categoryService.updateCategory(categoryId, request)
        );

        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository).findCategoryByName("Existing Name");
    }

    @Test
    @DisplayName("Should return category when found by id")
    void shouldReturnCategoryById() {
        UUID id = UUID.randomUUID();
        Category category = new Category(
                id, "Electronics", "Electronic items", Instant.now(), Instant.now()
        );
        CategoryResponseDto responseDto = CategoryResponseDto.builder()
                .categoryId(id)
                .name("Electronics")
                .description("Electronic items")
                .build();

        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));
        when(mapper.toDTO(category)).thenReturn(responseDto);

        CategoryResponseDto response = categoryService.getCategory(id);

        Assertions.assertEquals(id, response.getCategoryId());
        Assertions.assertEquals("Electronics", response.getName());
        verify(categoryRepository).findById(id);
    }

    @Test
    @DisplayName("Should throw error when category not found by id")
    void shouldThrowWhenCategoryNotFoundById() {
        UUID id = UUID.randomUUID();

        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        Assertions.assertThrows(
                CategoryNotFoundException.class,
                () -> categoryService.getCategory(id)
        );

        verify(categoryRepository).findById(id);
    }

    @Test
    @DisplayName("Should return category when found by name")
    void shouldReturnCategoryByName() {
        Category category = new Category(
                UUID.randomUUID(), "Electronics", "Electronic items",
                Instant.now(), Instant.now()
        );
        CategoryResponseDto responseDto = CategoryResponseDto.builder()
                .categoryId(category.getCategoryId())
                .name("Electronics")
                .description("Electronic items")
                .build();

        when(categoryRepository.findCategoryByName("Electronics")).thenReturn(Optional.of(category));
        when(mapper.toDTO(category)).thenReturn(responseDto);

        CategoryResponseDto response = categoryService.getCategory("Electronics");

        Assertions.assertEquals("Electronics", response.getName());
        verify(categoryRepository).findCategoryByName("Electronics");
    }

    @Test
    @DisplayName("Should throw error when category not found by name")
    void shouldThrowWhenCategoryNotFoundByName() {
        when(categoryRepository.findCategoryByName("missing")).thenReturn(Optional.empty());

        Assertions.assertThrows(
                CategoryNotFoundException.class,
                () -> categoryService.getCategory("missing")
        );

        verify(categoryRepository).findCategoryByName("missing");
    }

    @Test
    @DisplayName("Should get all categories successfully")
    void shouldGetAllCategoriesSuccessfully() {
        List<Category> categories = List.of(
                new Category(UUID.randomUUID(), "Electronics", "Electronic items", Instant.now(), Instant.now()),
                new Category(UUID.randomUUID(), "Books", "Book items", Instant.now(), Instant.now())
        );
        List<CategoryResponseDto> responseDtos = List.of(
                CategoryResponseDto.builder()
                        .categoryId(categories.get(0).getCategoryId())
                        .name("Electronics")
                        .description("Electronic items")
                        .build(),
                CategoryResponseDto.builder()
                        .categoryId(categories.get(1).getCategoryId())
                        .name("Books")
                        .description("Book items")
                        .build()
        );

        when(categoryRepository.findAll(PageRequest.of(0, 10))).thenReturn(new PageImpl<>(categories));
        when(mapper.toDTOList(categories)).thenReturn(responseDtos);

        List<CategoryResponseDto> responses = categoryService.getAllCategories(10, 0);

        Assertions.assertEquals(2, responses.size());
        Assertions.assertEquals("Electronics", responses.get(0).getName());
        Assertions.assertEquals("Books", responses.get(1).getName());
        verify(categoryRepository).findAll(PageRequest.of(0, 10));
    }

    @Test
    @DisplayName("Should return empty list when no categories found")
    void shouldReturnEmptyListWhenNoCategoriesFound() {
        when(categoryRepository.findAll(PageRequest.of(0, 10))).thenReturn(Page.empty());
        when(mapper.toDTOList(List.of())).thenReturn(List.of());

        List<CategoryResponseDto> responses = categoryService.getAllCategories(10, 0);

        Assertions.assertEquals(0, responses.size());
        verify(categoryRepository).findAll(PageRequest.of(0, 10));
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Should search categories by name successfully")
    void shouldSearchCategoriesByNameSuccessfully() {
        CategoryFilter filter = CategoryFilter.builder()
                .name("Elec")
                .build();

        List<Category> categories = List.of(
                new Category(UUID.randomUUID(), "Electronics", "Electronic items", Instant.now(), Instant.now())
        );

        Page<Category> categoryPage = new PageImpl<>(categories, PageRequest.of(0, 10), 1);

        CategoryResponseDto responseDto = CategoryResponseDto.builder()
                .categoryId(categories.get(0).getCategoryId())
                .name("Electronics")
                .description("Electronic items")
                .build();

        when(categoryRepository.findAll(any(Example.class), eq(PageRequest.of(0, 10)))).thenReturn(categoryPage);
        when(mapper.toDTOList(categories)).thenReturn(List.of(responseDto));

        List<CategoryResponseDto> responses = categoryService.getCategories(filter, 10, 0);

        Assertions.assertEquals(1, responses.size());
        Assertions.assertEquals("Electronics", responses.get(0).getName());
        verify(categoryRepository).findAll(any(Example.class), eq(PageRequest.of(0, 10)));
        verify(mapper).toDTOList(categories);
    }

    @Test
    @DisplayName("Should handle pagination correctly")
    void shouldHandlePaginationCorrectly() {
        List<Category> categories = List.of(
                new Category(UUID.randomUUID(), "Category1", "Description1", Instant.now(), Instant.now()),
                new Category(UUID.randomUUID(), "Category2", "Description2", Instant.now(), Instant.now())
        );
        List<CategoryResponseDto> responseDtos = List.of(
                CategoryResponseDto.builder()
                        .categoryId(categories.get(0).getCategoryId())
                        .name("Category1")
                        .description("Description1")
                        .build(),
                CategoryResponseDto.builder()
                        .categoryId(categories.get(1).getCategoryId())
                        .name("Category2")
                        .description("Description2")
                        .build()
        );

        when(categoryRepository.findAll(PageRequest.of(10, 5))).thenReturn(new PageImpl<>(categories));
        when(mapper.toDTOList(categories)).thenReturn(responseDtos);

        List<CategoryResponseDto> responses = categoryService.getAllCategories(5, 10);

        Assertions.assertEquals(2, responses.size());
        verify(categoryRepository).findAll(PageRequest.of(10, 5));
    }

    @Test
    @DisplayName("Should preserve created timestamp when updating category")
    void shouldPreserveCreatedTimestampWhenUpdating() {
        UUID categoryId = UUID.randomUUID();
        Instant createdAt = Instant.now().minusSeconds(86400);
        CategoryRequestDto request = new CategoryRequestDto("New Name", "New Description");
        Category existingCategory = new Category(
                categoryId, "Old Name", "Old Description",
                createdAt, Instant.now()
        );
        CategoryResponseDto responseDto = CategoryResponseDto.builder()
                .categoryId(categoryId)
                .name("New Name")
                .description("New Description")
                .createdAt(createdAt)
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.findCategoryByName("New Name")).thenReturn(Optional.empty());
        when(mapper.toDTO(existingCategory)).thenReturn(responseDto);

        CategoryResponseDto response = categoryService.updateCategory(categoryId, request);

        Assertions.assertEquals(createdAt, response.getCreatedAt());
        verify(mapper).toDTO(argThat(category ->
                category.getCreatedAt().equals(createdAt)
        ));
    }

    @Test
    @DisplayName("Should delete category successfully")
    void shouldDeleteCategorySuccessfully() {
        UUID categoryId = UUID.randomUUID();
        Category existingCategory = new Category(
                categoryId, "Electronics", "Electronic items",
                Instant.now(), Instant.now()
        );

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existingCategory));
        doNothing().when(categoryRepository).deleteById(categoryId);

        categoryService.deleteCategory(categoryId);

        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository).deleteById(categoryId);
    }

    @Test
    @DisplayName("Should throw error when deleting non-existing category")
    void shouldThrowWhenDeletingNonExistingCategory() {
        UUID categoryId = UUID.randomUUID();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        Assertions.assertThrows(
                CategoryNotFoundException.class,
                () -> categoryService.deleteCategory(categoryId)
        );

        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should throw error when deleting category with associated products")
    void shouldThrowWhenDeletingCategoryWithProducts() {
        UUID categoryId = UUID.randomUUID();
        Category existingCategory = new Category(
                categoryId, "Electronics", "Electronic items",
                Instant.now(), Instant.now()
        );

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existingCategory));
        doThrow(new CategoryDeletionException(categoryId.toString()))
                .when(categoryRepository).deleteById(categoryId);

        Assertions.assertThrows(
                CategoryDeletionException.class,
                () -> categoryService.deleteCategory(categoryId)
        );

        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository).deleteById(categoryId);
    }

}


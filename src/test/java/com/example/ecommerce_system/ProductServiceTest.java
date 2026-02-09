package com.example.ecommerce_system;

import com.example.ecommerce_system.dto.product.ProductFilter;
import com.example.ecommerce_system.dto.product.ProductRequestDto;
import com.example.ecommerce_system.dto.product.ProductResponseDto;
import com.example.ecommerce_system.exception.category.CategoryNotFoundException;
import com.example.ecommerce_system.exception.product.ProductNotFoundException;
import com.example.ecommerce_system.model.Category;
import com.example.ecommerce_system.model.Product;
import com.example.ecommerce_system.repository.CategoryRepository;
import com.example.ecommerce_system.repository.ProductRepository;
import com.example.ecommerce_system.service.ProductService;
import com.example.ecommerce_system.service.ReviewService;
import com.example.ecommerce_system.util.mapper.ProductMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ReviewService reviewService;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    @Test
    @DisplayName("Should create product successfully")
    void shouldCreateProductSuccessfully() {
        UUID categoryId = UUID.randomUUID();
        ProductRequestDto request = new ProductRequestDto(
                "Laptop",
                "Desc",
                1200.0,
                5,
                categoryId
        );

        Category category = new Category(categoryId, "Electronics", "Category Desc", Instant.now(), Instant.now());
        Product savedProduct = Product.builder()
                .productId(UUID.randomUUID())
                .name("Laptop")
                .description("Desc")
                .price(1200.0)
                .stockQuantity(5)
                .category(category)
                .reviews(new ArrayList<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        ProductResponseDto responseDto = ProductResponseDto.builder()
                .productId(savedProduct.getProductId())
                .name("Laptop")
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);
        when(productMapper.toDTO(savedProduct)).thenReturn(responseDto);

        ProductResponseDto response = productService.createProduct(request);

        Assertions.assertEquals("Laptop", response.getName());
        verify(categoryRepository).findById(categoryId);
        verify(productRepository).save(any(Product.class));
        verify(productMapper).toDTO(savedProduct);
    }

    @Test
    @DisplayName("Should throw error when creating product with non-existing category")
    void shouldThrowWhenCreatingProductWithNonExistingCategory() {
        UUID categoryId = UUID.randomUUID();
        ProductRequestDto request = new ProductRequestDto(
                "Laptop",
                null,
                null,
                null,
                categoryId
        );

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        Assertions.assertThrows(
                CategoryNotFoundException.class,
                () -> productService.createProduct(request)
        );

        verify(categoryRepository).findById(categoryId);
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get product by id successfully")
    void shouldGetProductByIdSuccessfully() {
        UUID id = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Category category = new Category(categoryId, "Electronics", "Desc", Instant.now(), Instant.now());
        Product product = Product.builder()
                .productId(id)
                .name("Phone")
                .description("Desc")
                .price(800.0)
                .stockQuantity(10)
                .category(category)
                .reviews(new ArrayList<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        ProductResponseDto responseDto = ProductResponseDto.builder()
                .productId(id)
                .name("Phone")
                .build();

        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productMapper.toDTO(product)).thenReturn(responseDto);

        ProductResponseDto response = productService.getProduct(id);

        Assertions.assertEquals(id, response.getProductId());
        Assertions.assertEquals("Phone", response.getName());
        verify(productRepository).findById(id);
        verify(productMapper).toDTO(product);
    }

    @Test
    @DisplayName("Should throw error when product not found by id")
    void shouldThrowWhenProductNotFoundById() {
        UUID id = UUID.randomUUID();

        when(productRepository.findById(id)).thenReturn(Optional.empty());

        Assertions.assertThrows(
                ProductNotFoundException.class,
                () -> productService.getProduct(id)
        );

        verify(productRepository).findById(id);
    }

    @Test
    @DisplayName("Should delete product successfully")
    void shouldDeleteProductSuccessfully() {
        UUID id = UUID.randomUUID();
        Category category = new Category(UUID.randomUUID(), "Electronics", "Desc", Instant.now(), Instant.now());
        Product product = Product.builder()
                .productId(id)
                .name("Laptop")
                .description("Desc")
                .price(1200.0)
                .stockQuantity(5)
                .category(category)
                .reviews(new ArrayList<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(productRepository.findById(id)).thenReturn(Optional.of(product));

        productService.deleteProduct(id);

        verify(productRepository).findById(id);
        verify(productRepository).deleteById(id);
    }

    @Test
    @DisplayName("Should throw error when deleting non-existing product")
    void shouldThrowWhenDeletingNonExistingProduct() {
        UUID id = UUID.randomUUID();

        when(productRepository.findById(id)).thenReturn(Optional.empty());

        Assertions.assertThrows(
                ProductNotFoundException.class,
                () -> productService.deleteProduct(id)
        );

        verify(productRepository).findById(id);
        verify(productRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should update product successfully")
    void shouldUpdateProductSuccessfully() {
        UUID id = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        ProductRequestDto request = new ProductRequestDto(
                "New Name",
                "New Desc",
                1500.0,
                8,
                categoryId
        );

        Category oldCategory = new Category(UUID.randomUUID(), "Old Category", "Desc", Instant.now(), Instant.now());
        Category newCategory = new Category(categoryId, "New Category", "Desc", Instant.now(), Instant.now());
        Product existing = Product.builder()
                .productId(id)
                .name("Old Name")
                .description("Old Desc")
                .price(1200.0)
                .stockQuantity(5)
                .category(oldCategory)
                .reviews(new ArrayList<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(productRepository.findById(id)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(newCategory));
        when(productMapper.toDTO(any(Product.class))).thenReturn(ProductResponseDto.builder().build());

        productService.updateProduct(id, request);

        verify(productRepository).findById(id);
        verify(categoryRepository).findById(categoryId);
        verify(productRepository).save(argThat(product ->
                product.getName().equals("New Name") &&
                        product.getDescription().equals("New Desc") &&
                        product.getPrice() == 1500.0 &&
                        product.getStockQuantity() == 8
        ));
    }

    @Test
    @DisplayName("Should throw error when updating non-existing product")
    void shouldThrowWhenUpdatingMissingProduct() {
        UUID id = UUID.randomUUID();
        ProductRequestDto request = new ProductRequestDto(
                "New",
                null,
                null,
                null,
                UUID.randomUUID()
        );

        when(productRepository.findById(id)).thenReturn(Optional.empty());

        Assertions.assertThrows(
                ProductNotFoundException.class,
                () -> productService.updateProduct(id, request)
        );

        verify(productRepository).findById(id);
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update only provided fields")
    void shouldUpdateOnlyProvidedFields() {
        UUID id = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        ProductRequestDto request = new ProductRequestDto(
                "New Name",
                null,
                null,
                null,
                null
        );

        Category category = new Category(categoryId, "Electronics", "Desc", Instant.now(), Instant.now());
        Product existing = Product.builder()
                .productId(id)
                .name("Old Name")
                .description("Old Desc")
                .price(1200.0)
                .stockQuantity(5)
                .category(category)
                .reviews(new ArrayList<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(productRepository.findById(id)).thenReturn(Optional.of(existing));
        when(productMapper.toDTO(any(Product.class))).thenReturn(ProductResponseDto.builder().build());

        productService.updateProduct(id, request);

        verify(productRepository).save(argThat(product ->
                product.getName().equals("New Name") &&
                        product.getDescription().equals("Old Desc") &&
                        product.getPrice() == 1200.0 &&
                        product.getStockQuantity() == 5 &&
                        product.getCategory().equals(category)
        ));
    }

    @Test
    @DisplayName("Should search products successfully")
    void shouldSearchProductsSuccessfully() {
        ProductFilter filter = new ProductFilter(null, null);
        Category category = new Category(UUID.randomUUID(), "Electronics", "Desc", Instant.now(), Instant.now());
        Product product = Product.builder()
                .productId(UUID.randomUUID())
                .name("Laptop")
                .description("Desc")
                .price(1200.0)
                .stockQuantity(5)
                .category(category)
                .reviews(new ArrayList<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        List<Product> products = List.of(product);

        ProductResponseDto responseDto = ProductResponseDto.builder()
                .productId(product.getProductId())
                .name("Laptop")
                .build();

        Page<Product> page = new PageImpl<>(products);
        when(productRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);
        when(productMapper.toDTOList(products)).thenReturn(List.of(responseDto));

        List<ProductResponseDto> result = productService.searchProducts(filter, 10, 0);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("Laptop", result.get(0).getName());
        verify(productRepository).findAll(any(Specification.class), any(PageRequest.class));
        verify(productMapper).toDTOList(products);
    }

    @Test
    @DisplayName("Should return empty list when no products match filter")
    void shouldReturnEmptyListWhenNoProductsMatchFilter() {
        ProductFilter filter = new ProductFilter(null, null);

        Page<Product> page = new PageImpl<>(List.of());
        when(productRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);
        when(productMapper.toDTOList(List.of())).thenReturn(List.of());

        List<ProductResponseDto> result = productService.searchProducts(filter, 10, 0);

        Assertions.assertEquals(0, result.size());
        verify(productRepository).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    @DisplayName("Should preserve created timestamp when updating")
    void shouldPreserveCreatedTimestampWhenUpdating() {
        UUID id = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Instant createdAt = Instant.now().minusSeconds(86400);
        ProductRequestDto request = new ProductRequestDto(
                "New Name",
                null,
                null,
                null,
                null
        );

        Category category = new Category(categoryId, "Electronics", "Desc", Instant.now(), Instant.now());
        Product existing = Product.builder()
                .productId(id)
                .name("Old Name")
                .description("Desc")
                .price(1200.0)
                .stockQuantity(5)
                .category(category)
                .reviews(new ArrayList<>())
                .createdAt(createdAt)
                .updatedAt(Instant.now())
                .build();

        when(productRepository.findById(id)).thenReturn(Optional.of(existing));
        when(productMapper.toDTO(any(Product.class))).thenReturn(ProductResponseDto.builder().build());

        productService.updateProduct(id, request);

        verify(productRepository).save(argThat(product ->
                product.getCreatedAt().equals(createdAt)
        ));
    }

    @Test
    @DisplayName("Should handle pagination in search")
    void shouldHandlePaginationInSearch() {
        ProductFilter filter = new ProductFilter(null, null);
        Category category = new Category(UUID.randomUUID(), "Electronics", "Desc", Instant.now(), Instant.now());
        Product product1 = Product.builder()
                .productId(UUID.randomUUID())
                .name("Phone1")
                .description("Desc")
                .price(800.0)
                .stockQuantity(10)
                .category(category)
                .reviews(new ArrayList<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        Product product2 = Product.builder()
                .productId(UUID.randomUUID())
                .name("Phone2")
                .description("Desc")
                .price(900.0)
                .stockQuantity(8)
                .category(category)
                .reviews(new ArrayList<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        List<Product> products = List.of(product1, product2);

        Page<Product> page = new PageImpl<>(products);
        when(productRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);
        when(productMapper.toDTOList(products)).thenReturn(List.of(
                ProductResponseDto.builder().name("Phone1").build(),
                ProductResponseDto.builder().name("Phone2").build()
        ));

        List<ProductResponseDto> result = productService.searchProducts(filter, 5, 10);

        Assertions.assertEquals(2, result.size());
        verify(productRepository).findAll(any(Specification.class), eq(PageRequest.of(10, 5)));
    }

    @Test
    @DisplayName("Should get all products successfully")
    void shouldGetAllProductsSuccessfully() {
        Category category = new Category(UUID.randomUUID(), "Electronics", "Desc", Instant.now(), Instant.now());
        Product product1 = Product.builder()
                .productId(UUID.randomUUID())
                .name("Product1")
                .description("Desc1")
                .price(100.0)
                .stockQuantity(10)
                .category(category)
                .reviews(new ArrayList<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        Product product2 = Product.builder()
                .productId(UUID.randomUUID())
                .name("Product2")
                .description("Desc2")
                .price(200.0)
                .stockQuantity(20)
                .category(category)
                .reviews(new ArrayList<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        List<Product> products = List.of(product1, product2);

        Page<Product> page = new PageImpl<>(products);
        when(productRepository.findAll(any(PageRequest.class))).thenReturn(page);
        when(productMapper.toDTOList(products)).thenReturn(List.of(
                ProductResponseDto.builder().name("Product1").build(),
                ProductResponseDto.builder().name("Product2").build()
        ));

        List<ProductResponseDto> result = productService.getAllProducts(10, 0);

        Assertions.assertEquals(2, result.size());
        verify(productRepository).findAll(any(PageRequest.class));
        verify(productMapper).toDTOList(products);
    }
}

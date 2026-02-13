package com.example.ecommerce_system;

import com.example.ecommerce_system.config.CacheConfig;
import com.example.ecommerce_system.dto.product.ProductFilter;
import com.example.ecommerce_system.dto.product.ProductRequestDto;
import com.example.ecommerce_system.dto.product.ProductResponseDto;
import com.example.ecommerce_system.dto.product.ProductWithReviewsDto;
import com.example.ecommerce_system.model.Category;
import com.example.ecommerce_system.model.Product;
import com.example.ecommerce_system.repository.CategoryRepository;
import com.example.ecommerce_system.repository.ProductRepository;
import com.example.ecommerce_system.service.ProductService;
import com.example.ecommerce_system.util.mapper.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        ProductService.class,
        CacheConfig.class
})
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.cache.type=caffeine",
        "spring.cache.caffeine.spec=maximumSize=100,expireAfterWrite=1m"
})
class ProductServiceCacheIntegrationTest {

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private CategoryRepository categoryRepository;

    @MockitoBean
    private ProductMapper productMapper;

    @Autowired
    private ProductService productService;

    @Autowired
    private CacheManager cacheManager;

    private UUID productId;
    private UUID categoryId;
    private Product testProduct;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        reset(productRepository, categoryRepository, productMapper);

        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });

        productId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        testCategory = Category.builder()
                .categoryId(categoryId)
                .name("Electronics")
                .description("Electronic devices")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        testProduct = Product.builder()
                .productId(productId)
                .name("Test Laptop")
                .description("A test laptop")
                .price(1500.0)
                .stockQuantity(10)
                .category(testCategory)
                .reviews(List.of())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should cache individual product retrieval")
    void shouldCacheIndividualProductRetrieval() {
        ProductResponseDto expectedResponse = ProductResponseDto.builder()
                .productId(productId)
                .name("Test Laptop")
                .description("A test laptop")
                .price(1500.0)
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(productMapper.toDTO(testProduct)).thenReturn(expectedResponse);

        ProductResponseDto firstCall = productService.getProduct(productId);

        ProductResponseDto secondCall = productService.getProduct(productId);

        assertNotNull(firstCall);
        assertNotNull(secondCall);
        assertEquals(firstCall.getProductId(), secondCall.getProductId());
        assertEquals("Test Laptop", firstCall.getName());

        verify(productRepository, times(1)).findById(productId);
        verify(productMapper, times(1)).toDTO(testProduct);
    }

    @Test
    @DisplayName("Should cache paginated product retrieval")
    void shouldCachePaginatedProductRetrieval() {
        int limit = 10;
        int offset = 0;
        PageRequest pageRequest = PageRequest.of(offset, limit);
        Page<Product> productPage = new PageImpl<>(List.of(testProduct), pageRequest, 1);

        ProductResponseDto expectedResponse = ProductResponseDto.builder()
                .productId(productId)
                .name("Test Laptop")
                .description("A test laptop")
                .price(1500.0)
                .build();

        when(productRepository.findAll(pageRequest)).thenReturn(productPage);
        when(productMapper.toDTO(testProduct)).thenReturn(expectedResponse);
        when(productMapper.toDTOList(List.of(testProduct))).thenReturn(List.of(expectedResponse));

        List<ProductResponseDto> firstCall = productService.getAllProducts(limit, offset);

        List<ProductResponseDto> secondCall = productService.getAllProducts(limit, offset);

        assertNotNull(firstCall);
        assertNotNull(secondCall);
        assertEquals(1, firstCall.size());
        assertEquals(1, secondCall.size());
        assertEquals("Test Laptop", firstCall.get(0).getName());

        verify(productRepository, times(1)).findAll(pageRequest);
        verify(productMapper, times(1)).toDTOList(List.of(testProduct));
    }

    @Test
    @DisplayName("Should cache product search results")
    @SuppressWarnings("unchecked")
    void shouldCacheProductSearchResults() {
        int limit = 5;
        int offset = 0;
        ProductFilter filter = new ProductFilter("laptop", null);
        PageRequest pageRequest = PageRequest.of(offset, limit);
        Page<Product> productPage = new PageImpl<>(List.of(testProduct), pageRequest, 1);

        ProductResponseDto expectedResponse = ProductResponseDto.builder()
                .productId(productId)
                .name("Test Laptop")
                .description("A test laptop")
                .price(1500.0)
                .build();

        when(productRepository.findAll(any(Specification.class), eq(pageRequest))).thenReturn(productPage);
        when(productMapper.toDTO(testProduct)).thenReturn(expectedResponse);
        when(productMapper.toDTOList(List.of(testProduct))).thenReturn(List.of(expectedResponse));

        List<ProductResponseDto> firstCall = productService.searchProducts(filter, limit, offset);

        List<ProductResponseDto> secondCall = productService.searchProducts(filter, limit, offset);

        assertNotNull(firstCall);
        assertNotNull(secondCall);
        assertEquals(1, firstCall.size());
        assertEquals(1, secondCall.size());
        assertEquals("Test Laptop", firstCall.get(0).getName());

        verify(productRepository, times(1)).findAll(any(Specification.class), eq(pageRequest));
        verify(productMapper, times(1)).toDTOList(List.of(testProduct));
    }

    @Test
    @DisplayName("Should cache products with reviews retrieval")
    void shouldCacheProductsWithReviewsRetrieval() {
        int limit = 5;
        int offset = 0;
        int reviewLimit = 3;
        PageRequest pageRequest = PageRequest.of(offset, limit);
        Page<Product> productPage = new PageImpl<>(List.of(testProduct), pageRequest, 1);

        ProductWithReviewsDto expectedResponse = ProductWithReviewsDto.builder()
                .productId(productId)
                .name("Test Laptop")
                .description("A test laptop")
                .price(1500.0)
                .reviews(List.of())
                .build();

        when(productRepository.findAllWithLimitedReviews(reviewLimit, pageRequest)).thenReturn(productPage);
        when(productMapper.toProductWithReviewsDTOList(List.of(testProduct))).thenReturn(List.of(expectedResponse));

        List<ProductWithReviewsDto> firstCall = productService.getAllProductsWithReviews(limit, offset, reviewLimit);

        List<ProductWithReviewsDto> secondCall = productService.getAllProductsWithReviews(limit, offset, reviewLimit);

        assertNotNull(firstCall);
        assertNotNull(secondCall);
        assertEquals(1, firstCall.size());
        assertEquals(1, secondCall.size());
        assertEquals("Test Laptop", firstCall.get(0).getName());

        verify(productRepository, times(1)).findAllWithLimitedReviews(reviewLimit, pageRequest);
        verify(productMapper, times(1)).toProductWithReviewsDTOList(List.of(testProduct));
    }

    @Test
    @DisplayName("Should evict cache when creating product")
    void shouldEvictCacheWhenCreatingProduct() {
        ProductRequestDto createRequest = new ProductRequestDto(
                "New Product",
                "Description",
                1200.0,
                5,
                categoryId
        );

        Product newProduct = Product.builder()
                .productId(UUID.randomUUID())
                .name("New Product")
                .description("Description")
                .price(1200.0)
                .stockQuantity(5)
                .category(testCategory)
                .reviews(List.of())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        int limit = 10;
        int offset = 0;
        PageRequest pageRequest = PageRequest.of(offset, limit);
        Page<Product> productPage = new PageImpl<>(List.of(testProduct), pageRequest, 1);

        ProductResponseDto testProductResponse = ProductResponseDto.builder()
                .productId(productId)
                .name("Test Laptop")
                .build();

        ProductResponseDto newProductResponse = ProductResponseDto.builder()
                .productId(newProduct.getProductId())
                .name("New Product")
                .build();

        when(productRepository.findAll(pageRequest)).thenReturn(productPage);
        when(productMapper.toDTO(testProduct)).thenReturn(testProductResponse);

        productService.getAllProducts(limit, offset);
        verify(productRepository, times(1)).findAll(pageRequest);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));
        when(productRepository.save(any(Product.class))).thenReturn(newProduct);
        when(productMapper.toDTO(newProduct)).thenReturn(newProductResponse);

        productService.createProduct(createRequest);

        Page<Product> updatedProductPage = new PageImpl<>(List.of(testProduct, newProduct), pageRequest, 2);
        when(productRepository.findAll(pageRequest)).thenReturn(updatedProductPage);

        productService.getAllProducts(limit, offset);

        verify(productRepository, times(2)).findAll(pageRequest);
    }

    @Test
    @DisplayName("Should evict cache when updating product")
    void shouldEvictCacheWhenUpdatingProduct() {
        ProductRequestDto updateRequest = new ProductRequestDto(
                "Updated Laptop",
                "Updated description",
                1800.0,
                15,
                null
        );

        Product updatedProduct = Product.builder()
                .productId(productId)
                .name("Updated Laptop")
                .description("Updated description")
                .price(1800.0)
                .stockQuantity(15)
                .category(testCategory)
                .reviews(List.of())
                .createdAt(testProduct.getCreatedAt())
                .updatedAt(Instant.now())
                .build();

        ProductResponseDto originalResponse = ProductResponseDto.builder()
                .productId(productId)
                .name("Test Laptop")
                .build();

        ProductResponseDto updatedResponse = ProductResponseDto.builder()
                .productId(productId)
                .name("Updated Laptop")
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(productMapper.toDTO(testProduct)).thenReturn(originalResponse);

        productService.getProduct(productId);
        verify(productRepository, times(1)).findById(productId);

        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);
        when(productMapper.toDTO(updatedProduct)).thenReturn(updatedResponse);

        productService.updateProduct(productId, updateRequest);

        when(productRepository.findById(productId)).thenReturn(Optional.of(updatedProduct));

        productService.getProduct(productId);

        verify(productRepository, times(3)).findById(productId);
    }

    @Test
    @DisplayName("Should evict cache when deleting product")
    void shouldEvictCacheWhenDeletingProduct() {
        ProductResponseDto expectedResponse = ProductResponseDto.builder()
                .productId(productId)
                .name("Test Laptop")
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(productMapper.toDTO(testProduct)).thenReturn(expectedResponse);

        productService.getProduct(productId);
        verify(productRepository, times(1)).findById(productId);

        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        doNothing().when(productRepository).deleteById(productId);

        productService.deleteProduct(productId);

        verify(productRepository, times(1)).deleteById(productId);
        verify(productRepository, times(2)).findById(productId);
    }

    @Test
    @DisplayName("Should use different cache entries for different parameters")
    void shouldUseDifferentCacheEntriesForDifferentParameters() {
        int limit1 = 5, offset1 = 0;
        int limit2 = 10, offset2 = 1;

        PageRequest pageRequest1 = PageRequest.of(offset1, limit1);
        PageRequest pageRequest2 = PageRequest.of(offset2, limit2);

        Page<Product> productPage1 = new PageImpl<>(List.of(testProduct), pageRequest1, 1);
        Page<Product> productPage2 = new PageImpl<>(List.of(), pageRequest2, 0);

        ProductResponseDto expectedResponse = ProductResponseDto.builder()
                .productId(productId)
                .name("Test Laptop")
                .build();

        when(productRepository.findAll(pageRequest1)).thenReturn(productPage1);
        when(productRepository.findAll(pageRequest2)).thenReturn(productPage2);
        when(productMapper.toDTO(testProduct)).thenReturn(expectedResponse);
        when(productMapper.toDTOList(List.of(testProduct))).thenReturn(List.of(expectedResponse));

        List<ProductResponseDto> firstCall = productService.getAllProducts(limit1, offset1);
        List<ProductResponseDto> secondCall = productService.getAllProducts(limit2, offset2);

        List<ProductResponseDto> thirdCall = productService.getAllProducts(limit1, offset1);
        List<ProductResponseDto> fourthCall = productService.getAllProducts(limit2, offset2);

        assertEquals(1, firstCall.size());
        assertEquals(0, secondCall.size());
        assertEquals(1, thirdCall.size());
        assertEquals(0, fourthCall.size());

        verify(productRepository, times(1)).findAll(pageRequest1);
        verify(productRepository, times(1)).findAll(pageRequest2);
    }

    @Test
    @DisplayName("Should verify cache manager configuration")
    void shouldVerifyCacheManagerConfiguration() {
        var cacheNames = cacheManager.getCacheNames();

        assertTrue(cacheNames.contains("products"));
        assertTrue(cacheNames.contains("paginated"));

        assertNotNull(cacheManager.getCache("products"));
        assertNotNull(cacheManager.getCache("paginated"));
    }
}


package com.example.ecommerce_system.service;

import com.example.ecommerce_system.dto.product.ProductFilter;
import com.example.ecommerce_system.dto.product.ProductRequestDto;
import com.example.ecommerce_system.dto.product.ProductResponseDto;
import com.example.ecommerce_system.dto.product.ProductWithReviewsDto;
import com.example.ecommerce_system.exception.category.CategoryNotFoundException;
import com.example.ecommerce_system.exception.product.ProductNotFoundException;
import com.example.ecommerce_system.model.Category;
import com.example.ecommerce_system.model.Product;
import com.example.ecommerce_system.repository.CategoryRepository;
import com.example.ecommerce_system.repository.ProductRepository;
import com.example.ecommerce_system.util.ProductSpecification;
import com.example.ecommerce_system.util.mapper.ProductMapper;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    /**
     * Create a new product.
     * Validates that the category exists before creating the product.
     */
    @CacheEvict(value = {"products", "paginated"}, allEntries = true)
    public ProductResponseDto createProduct(ProductRequestDto request) {
        var category = getCategory(request.getCategoryId());

        Product product = Product.builder()
                .productId(UUID.randomUUID())
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStock())
                .category(category)
                .reviews(List.of())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        var saved = productRepository.save(product);
        return productMapper.toDTO(saved);
    }

    private Category getCategory(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId.toString()));
    }

    @Cacheable(value = "products", key = "#productId")
    public ProductResponseDto getProduct(UUID productId) {
        var product = retrieveProductFromRepository(productId);
        return productMapper.toDTO(product);
    }

    private Product retrieveProductFromRepository(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id.toString()));
    }

    /**
     * Retrieve all products with pagination.
     */
    @Cacheable(value = "paginated", key = "'all_products_' + #limit + '_' + #offset")
    public List<ProductResponseDto> getAllProducts(int limit, int offset) {
        List<Product> products = productRepository.findAll(PageRequest.of(offset, limit)).getContent();
        return productMapper.toDTOList(products);
    }

    /**
     * Delete a product by ID.
     * Validates that the product exists before deletion.
     */
    @CacheEvict(value = {"products", "paginated"}, allEntries = true)
    public void deleteProduct(UUID productId) {
        var existing = retrieveProductFromRepository(productId);
        productRepository.deleteById(existing.getProductId());
    }

    /**
     * Search for products using a filter with pagination.
     */
    @Cacheable(value = "paginated", key = "'search_products_' + #filter.toString() + '_' + #limit + '_' + #offset")
    public List<ProductResponseDto> searchProducts(ProductFilter filter, int limit, int offset) {
        Specification<Product> spec = ProductSpecification.buildSpecification(filter);
        List<Product> products = productRepository.findAll(spec, PageRequest.of(offset, limit)).getContent();
        return productMapper.toDTOList(products);
    }

    /**
     * Update an existing product.
     * Validates product existence and merges provided fields with existing values.
     */
    @CacheEvict(value = {"products", "paginated"}, allEntries = true)
    public ProductResponseDto updateProduct(UUID productId, ProductRequestDto request) {
        var existingProduct = retrieveProductFromRepository(productId);

        Product updated = Product.builder()
                .productId(existingProduct.getProductId())
                .name(request.getName() != null ? request.getName() : existingProduct.getName())
                .description(request.getDescription() != null ? request.getDescription() : existingProduct.getDescription())
                .price(request.getPrice() != null ? request.getPrice() : existingProduct.getPrice())
                .stockQuantity(request.getStock() != null ? request.getStock() : existingProduct.getStockQuantity())
                .category(request.getCategoryId() != null ? getCategory(request.getCategoryId()) : existingProduct.getCategory())
                .reviews(existingProduct.getReviews())
                .createdAt(existingProduct.getCreatedAt())
                .updatedAt(Instant.now())
                .build();

        productRepository.save(updated);
        return productMapper.toDTO(updated);
    }

    /**
     * Get all products with their categories and reviews.
     * Each product includes a limited number of reviews based on reviewLimit parameter.
     */
    @Cacheable(value = "paginated", key = "'products_with_reviews_' + #limit + '_' + #offset + '_' + #reviewLimit")
    public List<ProductWithReviewsDto> getAllProductsWithReviews(int limit, int offset, int reviewLimit) {
        var productsPage = productRepository.findAllWithLimitedReviews(
                reviewLimit,
                PageRequest.of(offset, limit)
        );
        return productMapper.toProductWithReviewsDTOList(productsPage.getContent());
    }
}

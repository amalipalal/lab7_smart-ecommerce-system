package com.example.ecommerce_system.controller.rest;

import com.example.ecommerce_system.dto.product.ProductFilter;
import com.example.ecommerce_system.dto.SuccessResponseDto;
import com.example.ecommerce_system.dto.product.CreateProductRequest;
import com.example.ecommerce_system.dto.product.ProductRequestDto;
import com.example.ecommerce_system.dto.product.ProductResponseDto;
import com.example.ecommerce_system.dto.product.UpdateProductRequest;
import com.example.ecommerce_system.dto.review.ReviewResponseDto;
import com.example.ecommerce_system.service.ProductService;
import com.example.ecommerce_system.service.ReviewService;
import com.example.ecommerce_system.util.handler.SuccessResponseHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@Validated
@RequestMapping("/admin/products")
@PreAuthorize("hasRole('ADMIN')")
public class ProductAdminController {
    private final ProductService productService;
    private final ReviewService reviewService;

    @Operation(summary = "Retrieve all products")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "All products retrieved")
    })
    @GetMapping
    public SuccessResponseDto<List<ProductResponseDto>> getAllProducts(
            @RequestParam @Min(1) int limit,
            @RequestParam @Min(0) int offset,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) @PositiveOrZero Double minPrice,
            @RequestParam(required = false) @PositiveOrZero Double maxPrice,
            @RequestParam(required = false) @Min(0) Integer minStock,
            @RequestParam(required = false) @Min(0) Integer maxStock
    ) {
        ProductFilter filter = ProductFilter.builder()
                .name(name)
                .description(description)
                .categoryId(categoryId)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .minStock(minStock)
                .maxStock(maxStock)
                .build();

        List<ProductResponseDto> products = filter.isEmpty()
                ? productService.getAllProducts(limit, offset)
                : productService.searchProducts(filter, limit, offset);

        return SuccessResponseHandler.generateSuccessResponse(HttpStatus.OK, products);
    }

    @Operation(summary = "Retrieve a single product by productId")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A single product retrieved"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/{id}")
    public SuccessResponseDto<ProductResponseDto> getProductId(@PathVariable UUID id) {
        var product = productService.getProduct(id);
        return SuccessResponseHandler.generateSuccessResponse(HttpStatus.OK, product);
    }

    @Operation(summary = "Retrieve a single product's list of reviews")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A list of reviews"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/{id}/reviews")
    public SuccessResponseDto<List<ReviewResponseDto>> getProductWithReviews(
            @PathVariable UUID id,
            @RequestParam int limit,
            @RequestParam int offset
    ) {
        var product = reviewService.getReviewsByProduct(id, limit, offset);
        return SuccessResponseHandler.generateSuccessResponse(HttpStatus.OK, product);
    }

    @Operation(summary = "Create a new product")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created"),
            @ApiResponse(responseCode = "400", description = "Bad request")
    })
    @PostMapping
    public SuccessResponseDto<ProductResponseDto> addProduct(
            @RequestBody @Validated(CreateProductRequest.class) ProductRequestDto product
    ) {
        var productCreated = productService.createProduct(product);
        return SuccessResponseHandler.generateSuccessResponse(HttpStatus.CREATED, productCreated);
    }

    @Operation(summary = "Update a product by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product updated"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @PatchMapping("/{id}")
    public SuccessResponseDto<ProductResponseDto> updateProduct(
            @PathVariable UUID id,
            @RequestBody @Validated(UpdateProductRequest.class) ProductRequestDto update
    ) {
        var productCreated = productService.updateProduct(id, update);
        return SuccessResponseHandler.generateSuccessResponse(HttpStatus.OK, productCreated);
    }

    @Operation(summary = "Delete a product by id")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Product deleted"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "409", description = "Product cannot be deleted")
    })
    @DeleteMapping("/{id}")
    public SuccessResponseDto<Void> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return SuccessResponseHandler.generateSuccessResponse(HttpStatus.NO_CONTENT, null);
    }

}

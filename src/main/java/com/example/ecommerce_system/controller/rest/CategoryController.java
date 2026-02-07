package com.example.ecommerce_system.controller.rest;

import com.example.ecommerce_system.dto.*;
import com.example.ecommerce_system.dto.category.CategoryRequestDto;
import com.example.ecommerce_system.dto.category.CategoryResponseDto;
import com.example.ecommerce_system.dto.category.CreateCategoryRequest;
import com.example.ecommerce_system.dto.category.UpdateCategoryRequest;
import com.example.ecommerce_system.service.CategoryService;
import com.example.ecommerce_system.util.handler.SuccessResponseHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@Validated
@AllArgsConstructor
@RequestMapping("/categories")
public class CategoryController {
    private final CategoryService categoryService;

    @Operation(summary="Retrieve all categories")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "All categories retrieved"),
    })
    @GetMapping
    public SuccessResponseDto<List<CategoryResponseDto>> getAllCategories(
            @RequestParam @Min(1) @Max(100) int limit,
            @RequestParam @Min(0) int offset)
    {
        List<CategoryResponseDto> categories = categoryService.getAllCategories(limit, offset);
        return SuccessResponseHandler.generateSuccessResponse(HttpStatus.OK, categories);
    }

    @Operation(summary = "Retrieve a single category by categoryId")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A single category retrieved")
    })
    @GetMapping("/{id}")
    public SuccessResponseDto<CategoryResponseDto> getCategoryById(@PathVariable UUID id) {
        CategoryResponseDto category = categoryService.getCategory(id);
        return SuccessResponseHandler.generateSuccessResponse(HttpStatus.OK, category);
    }

    @Operation(summary = "Retrieve categories with name containing query")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Find all categories with names containing the query")
    })
    @GetMapping("/search")
    public SuccessResponseDto<List<CategoryResponseDto>> searchCategoriesByName(
            @RequestParam @NotBlank String query,
            @RequestParam @Min(1) @Max(100) int limit,
            @RequestParam @Min(0) int offset
    ) {
        List<CategoryResponseDto> categories = categoryService.getCategories(query, limit, offset);
        return SuccessResponseHandler.generateSuccessResponse(HttpStatus.OK, categories);
    }
}

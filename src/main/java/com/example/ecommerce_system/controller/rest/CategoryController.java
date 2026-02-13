package com.example.ecommerce_system.controller.rest;

import com.example.ecommerce_system.dto.*;
import com.example.ecommerce_system.dto.category.*;
import com.example.ecommerce_system.service.CategoryService;
import com.example.ecommerce_system.util.handler.SuccessResponseHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
            @RequestParam @Min(0) int offset,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description
    ) {
        var filter = CategoryFilter.builder()
                .name(name)
                .description(description)
                .build();

        List<CategoryResponseDto> categories = filter.isEmpty()
                ? categoryService.getAllCategories(limit, offset)
                : categoryService.getCategories(filter, limit, offset);
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
}

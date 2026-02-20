package com.example.ecommerce_system.controller.rest;

import com.example.ecommerce_system.dto.SuccessResponseDto;
import com.example.ecommerce_system.dto.category.CategoryRequestDto;
import com.example.ecommerce_system.dto.category.CategoryResponseDto;
import com.example.ecommerce_system.dto.category.CreateCategoryRequest;
import com.example.ecommerce_system.dto.category.UpdateCategoryRequest;
import com.example.ecommerce_system.service.CategoryService;
import com.example.ecommerce_system.util.handler.SuccessResponseHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@Validated
@AllArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/admin/categories")
public class CategoryAdminController {
    private final CategoryService categoryService;

    @Operation(summary = "Add a category")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category created"),
            @ApiResponse(responseCode = "404", description = "Category not found")
    })
    @PostMapping
    public SuccessResponseDto<CategoryResponseDto> addCategory(
            @RequestBody @Validated(CreateCategoryRequest.class) CategoryRequestDto category
    ) {
        CategoryResponseDto categoryCreated = categoryService.createCategory(category);
        return SuccessResponseHandler.generateSuccessResponse(HttpStatus.CREATED, categoryCreated);
    }

    @Operation(summary = "Update a category by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category updated"),
            @ApiResponse(responseCode = "404", description = "Category not found")
    })
    @PatchMapping("/{id}")
    public SuccessResponseDto<CategoryResponseDto> updateCategory(
            @PathVariable UUID id,
            @RequestBody @Validated(UpdateCategoryRequest.class) CategoryRequestDto update
    ) {
        CategoryResponseDto updatedCategory = categoryService.updateCategory(id, update);
        return SuccessResponseHandler.generateSuccessResponse(HttpStatus.OK, updatedCategory);
    }

    @Operation(summary = "Delete a category by id")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Category deleted"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "404", description = "Category not found"),
            @ApiResponse(responseCode = "409", description = "Category has associated products and cannot be deleted")
    })
    @DeleteMapping("/{id}")
    public SuccessResponseDto<Void> deleteCategory(@PathVariable UUID id) {
        categoryService.deleteCategory(id);
        return SuccessResponseHandler.generateSuccessResponse(HttpStatus.NO_CONTENT, null);
    }
}

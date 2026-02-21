package com.example.ecommerce_system.controller.rest;

import com.example.ecommerce_system.dto.SuccessResponseDto;
import com.example.ecommerce_system.dto.customer.CustomerRequestDto;
import com.example.ecommerce_system.dto.customer.CustomerResponseDto;
import com.example.ecommerce_system.dto.review.ReviewResponseDto;
import com.example.ecommerce_system.service.CustomerService;
import com.example.ecommerce_system.service.ReviewService;
import com.example.ecommerce_system.util.handler.SuccessResponseHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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
@RequestMapping("/admin/customers")
@PreAuthorize("hasRole('ADMIN')")
public class CustomerAdminController {
    private final CustomerService customerService;
    private final ReviewService reviewService;

    @Operation(summary = "Retrieve all customers")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "All customers retrieved")
    })
    @GetMapping
    public SuccessResponseDto<List<CustomerResponseDto>> getAllCustomers(
            @RequestParam @Min(1) int limit,
            @RequestParam @Min(0) int offset
    ) {
        List<CustomerResponseDto> customers = customerService.getAllCustomers(limit, offset);
        return SuccessResponseHandler.generateSuccessResponse(HttpStatus.OK, customers);
    }

    @Operation(summary = "Retrieve a single customer by customerId")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A single customer retrieved"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    @GetMapping("/{id}")
    public SuccessResponseDto<CustomerResponseDto> getCustomerId(@PathVariable UUID id) {
        var customer = customerService.getCustomer(id);
        return SuccessResponseHandler.generateSuccessResponse(HttpStatus.OK, customer);
    }

    @Operation(summary = "Search customers by query matching first name, last name, or email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customers matching search criteria retrieved")
    })
    @GetMapping("/search")
    public SuccessResponseDto<List<CustomerResponseDto>> searchCustomers(
            @RequestParam String query,
            @RequestParam @Min(1) int limit,
            @RequestParam @Min(0) int offset
    ) {
        List<CustomerResponseDto> customers = customerService.searchCustomers(query, limit, offset);
        return SuccessResponseHandler.generateSuccessResponse(HttpStatus.OK, customers);
    }

    @Operation(summary = "Update a customer's phone and/or active status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer updated"),
            @ApiResponse(responseCode = "400", description = "Bad request - at least one field must be provided"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    @PatchMapping("/{id}")
    public SuccessResponseDto<CustomerResponseDto> updateCustomer(
            @PathVariable UUID id,
            @RequestBody @Valid CustomerRequestDto update
    ) {
        if (update.getPhone() == null && update.getActive() == null) {
            throw new IllegalArgumentException("At least one field (phone or isActive) must be provided for update");
        }

        var customerUpdated = customerService.updateCustomer(id, update);
        return SuccessResponseHandler.generateSuccessResponse(HttpStatus.OK, customerUpdated);
    }

    @Operation(summary = "Retrieve all reviews made by a specific customer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer reviews retrieved"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    @GetMapping("/{id}/reviews")
    public SuccessResponseDto<List<ReviewResponseDto>> getCustomerReviews(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "10") @Min(1) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset
    ) {
        List<ReviewResponseDto> reviews = reviewService.getReviewsByCustomer(id, limit, offset);
        return SuccessResponseHandler.generateSuccessResponse(HttpStatus.OK, reviews);
    }
}

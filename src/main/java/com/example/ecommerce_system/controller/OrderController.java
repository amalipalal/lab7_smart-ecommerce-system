package com.example.ecommerce_system.controller;

import com.example.ecommerce_system.dto.SuccessResponseDto;
import com.example.ecommerce_system.dto.orders.OrderRequestDto;
import com.example.ecommerce_system.dto.orders.OrderResponseDto;
import com.example.ecommerce_system.dto.orders.UpdateOrderRequest;
import com.example.ecommerce_system.service.OrderService;
import com.example.ecommerce_system.util.SuccessResponseHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@Validated
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orderService;

    @Operation(summary = "Retrieve all orders")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "All orders retrieved")
    })
    @GetMapping
    public SuccessResponseDto<List<OrderResponseDto>> getAllOrders(
            @RequestParam @Min(1) int limit,
            @RequestParam @Min(0) int offset
    ) {
        List<OrderResponseDto> orders = orderService.getAllOrders(limit, offset);
        return SuccessResponseHandler.generateSuccessResponse(HttpStatus.OK, orders);
    }

    @Operation(summary = "Retrieve a single order by orderId")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A single order retrieved"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/{id}")
    public SuccessResponseDto<OrderResponseDto> getOrder(@PathVariable UUID id) {
        var order = orderService.getOrder(id);
        return SuccessResponseHandler.generateSuccessResponse(HttpStatus.OK, order);
    }

    @Operation(summary = "Update order status by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order status updated"),
            @ApiResponse(responseCode = "400", description = "Bad request"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @PatchMapping("/{id}")
    public SuccessResponseDto<OrderResponseDto> updateOrderStatus(
            @PathVariable UUID id,
            @RequestBody @Validated(UpdateOrderRequest.class) OrderRequestDto request
    ) {
        var updatedOrder = orderService.updateOrderStatus(id, request);
        return SuccessResponseHandler.generateSuccessResponse(HttpStatus.OK, updatedOrder);
    }
}

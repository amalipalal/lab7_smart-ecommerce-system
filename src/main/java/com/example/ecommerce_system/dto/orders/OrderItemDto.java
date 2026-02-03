package com.example.ecommerce_system.dto.orders;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@AllArgsConstructor
@Builder
@Data
public class OrderItemDto {
    @NotNull(
            groups = OrderItemResponse.class,
            message = "orderItemId is required"
    )
    private UUID orderItemId;

    @NotNull(
            groups = {OrderItemRequest.class, OrderItemResponse.class, CreateOrderRequest.class},
            message = "product id is required")
    private UUID productId;

    @Positive(
            groups = {OrderItemRequest.class, OrderItemResponse.class, CreateOrderRequest.class},
            message = "quantity must be greater than 0"
    )
    @NotNull(
            groups = {OrderItemRequest.class, OrderItemResponse.class, CreateOrderRequest.class},
            message = "quantity is required"
    )
    private Integer quantity;

    @DecimalMin(
            groups = {OrderItemRequest.class, OrderItemResponse.class, CreateOrderRequest.class},
            value = "0.01",
            message = "price must be at least 0.01"
    )
    @NotNull(
            groups = {OrderItemRequest.class, OrderItemResponse.class, CreateOrderRequest.class},
            message = "price is required"
    )
    private Double price;
}

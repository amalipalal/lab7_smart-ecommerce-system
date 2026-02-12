package com.example.ecommerce_system.dto.orders;

import com.example.ecommerce_system.enums.OrderStatusType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Builder
@Getter
public class OrderRequestDto {
    @NotNull(
            groups = UpdateOrderRequest.class,
            message = "status is required and cannot be empty"
    )
    private OrderStatusType status;

    @NotBlank(
            groups = CreateOrderRequest.class,
            message = "country is required and cannot be empty"
    )
    private String country;

    @NotBlank(
            groups = CreateOrderRequest.class,
            message = "city is required and cannot be empty"
    )
    private String city;

    @NotBlank(
            groups = CreateOrderRequest.class,
            message = "postalCode is required and cannot be empty"
    )
    private String postalCode;

    @NotEmpty(
            groups = CreateOrderRequest.class,
            message = "items is required"
    )
    @Valid
    private List<OrderItemDto> items;
}

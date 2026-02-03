package com.example.ecommerce_system.dto.orders;

import com.example.ecommerce_system.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Builder
@Data
public class OrderResponseDto {
    private UUID orderId;
    private OrderStatus status;
    private Instant orderDate;
    private double totalAmount;
    private String shippingCountry;
    private String shippingCity;
    private String shippingPostalCode;
    private List<OrderItemDto> items;
}

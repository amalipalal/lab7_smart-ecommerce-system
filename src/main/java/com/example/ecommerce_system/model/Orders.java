package com.example.ecommerce_system.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Builder
public class Orders {
    private UUID orderId;
    private UUID customerId;
    private OrderStatus status;
    private Instant orderDate;
    private double totalAmount;
    private String shippingCountry;
    private String shippingCity;
    private String shippingPostalCode;
}

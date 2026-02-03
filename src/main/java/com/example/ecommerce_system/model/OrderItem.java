package com.example.ecommerce_system.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@AllArgsConstructor
@Getter
@Builder
public class OrderItem {
    private UUID orderItemId;
    private UUID orderId;
    private UUID productId;
    private int quantity;
    private double priceAtPurchase;
}

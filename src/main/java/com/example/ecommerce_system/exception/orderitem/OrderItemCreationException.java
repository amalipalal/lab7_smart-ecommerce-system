package com.example.ecommerce_system.exception.orderitem;

public class OrderItemCreationException extends RuntimeException {
    public OrderItemCreationException(String identifier) {
        super("Failed to create order item '" + identifier + "'.");
    }
}

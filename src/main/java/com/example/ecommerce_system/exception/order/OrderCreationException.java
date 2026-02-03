package com.example.ecommerce_system.exception.order;

public class OrderCreationException extends RuntimeException {
    public OrderCreationException(String identifier) {
        super("Failed to create order '" + identifier + "'.");
    }
}

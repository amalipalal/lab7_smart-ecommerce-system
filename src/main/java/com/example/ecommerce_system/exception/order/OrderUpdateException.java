package com.example.ecommerce_system.exception.order;

public class OrderUpdateException extends RuntimeException {
    public OrderUpdateException(String identifier) {
        super("Failed to update order '" + identifier + "'.");
    }
}

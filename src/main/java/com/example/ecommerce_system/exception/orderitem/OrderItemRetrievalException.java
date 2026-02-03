package com.example.ecommerce_system.exception.orderitem;

public class OrderItemRetrievalException extends RuntimeException {
    public OrderItemRetrievalException(String identifier) {
        super("Failed to retrieve order items for '" + identifier + "'.");
    }
}

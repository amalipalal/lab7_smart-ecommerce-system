package com.example.ecommerce_system.exception.order;

public class OrderRetrievalException extends RuntimeException {
    public OrderRetrievalException(String identifier) {
        super("Failed to retrieve order '" + identifier + "'.");
    }
}

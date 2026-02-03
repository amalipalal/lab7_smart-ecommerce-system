package com.example.ecommerce_system.exception.order;

public class DeleteOrderException extends RuntimeException {
    public DeleteOrderException(String identifier) {
        super("Failed to delete order '" + identifier + "'.");
    }
}

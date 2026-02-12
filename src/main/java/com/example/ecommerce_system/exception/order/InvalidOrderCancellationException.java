package com.example.ecommerce_system.exception.order;

public class InvalidOrderCancellationException extends RuntimeException {
    public InvalidOrderCancellationException(String message) {
        super(message);
    }
}

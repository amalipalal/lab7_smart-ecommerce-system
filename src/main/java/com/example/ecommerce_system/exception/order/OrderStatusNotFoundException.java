package com.example.ecommerce_system.exception.order;

public class OrderStatusNotFoundException extends RuntimeException {
    public OrderStatusNotFoundException(String statusName) {
        super("Order status '" + statusName + "' not found in the system.");
    }
}

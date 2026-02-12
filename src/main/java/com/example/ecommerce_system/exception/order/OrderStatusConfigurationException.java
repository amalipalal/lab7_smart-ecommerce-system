package com.example.ecommerce_system.exception.order;

public class OrderStatusConfigurationException extends RuntimeException {
    public OrderStatusConfigurationException(String statusName) {
        super("Required order status '" + statusName + "' is not configured in the system.");
    }
}

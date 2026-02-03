package com.example.ecommerce_system.exception.order;

public class OrderDoesNotExist extends RuntimeException {
    public OrderDoesNotExist(String identifier) {
        super("The order '" + identifier + "' does not exist.");
    }
}

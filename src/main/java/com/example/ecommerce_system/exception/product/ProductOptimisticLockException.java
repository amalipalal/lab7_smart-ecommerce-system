package com.example.ecommerce_system.exception.product;

public class ProductOptimisticLockException extends RuntimeException {
    public ProductOptimisticLockException(String productId) {
        super("Optimistic lock exception occurred for product: " + productId +
              ". The product was modified by another transaction.");
    }

    public ProductOptimisticLockException(String productId, Throwable cause) {
        super("Optimistic lock exception occurred for product: " + productId +
              ". The product was modified by another transaction.", cause);
    }
}

package com.example.ecommerce_system.util;

import com.example.ecommerce_system.dto.ErrorResponseDto;
import com.example.ecommerce_system.exception.order.DeleteOrderException;
import com.example.ecommerce_system.exception.order.OrderCreationException;
import com.example.ecommerce_system.exception.order.OrderDoesNotExist;
import com.example.ecommerce_system.exception.order.OrderRetrievalException;
import com.example.ecommerce_system.exception.order.OrderUpdateException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class OrderResponseHandler {

    @ExceptionHandler(OrderDoesNotExist.class)
    public ResponseEntity<ErrorResponseDto<String>> handleOrderNotFound(OrderDoesNotExist exception) {
        return ErrorResponseHandler.generateErrorMessage(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                exception.getClass().getSimpleName());
    }

    @ExceptionHandler(OrderCreationException.class)
    public ResponseEntity<ErrorResponseDto<String>> handleOrderCreation(OrderCreationException exception) {
        return ErrorResponseHandler.generateErrorMessage(
                HttpStatus.INTERNAL_SERVER_ERROR,
                exception.getMessage(),
                exception.getClass().getSimpleName());
    }

    @ExceptionHandler(OrderUpdateException.class)
    public ResponseEntity<ErrorResponseDto<String>> handleOrderUpdate(OrderUpdateException exception) {
        return ErrorResponseHandler.generateErrorMessage(
                HttpStatus.INTERNAL_SERVER_ERROR,
                exception.getMessage(),
                exception.getClass().getSimpleName());
    }

    @ExceptionHandler(DeleteOrderException.class)
    public ResponseEntity<ErrorResponseDto<String>> handleOrderDeletion(DeleteOrderException exception) {
        return ErrorResponseHandler.generateErrorMessage(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                exception.getClass().getSimpleName());
    }

    @ExceptionHandler(OrderRetrievalException.class)
    public ResponseEntity<ErrorResponseDto<String>> handleOrderRetrieval(OrderRetrievalException exception) {
        return ErrorResponseHandler.generateErrorMessage(
                HttpStatus.INTERNAL_SERVER_ERROR,
                exception.getMessage(),
                exception.getClass().getSimpleName());
    }
}

package com.example.ecommerce_system.service;

import com.example.ecommerce_system.dto.orders.OrderItemDto;
import com.example.ecommerce_system.dto.orders.OrderRequestDto;
import com.example.ecommerce_system.dto.orders.OrderResponseDto;
import com.example.ecommerce_system.exception.customer.CustomerNotFoundException;
import com.example.ecommerce_system.exception.order.InvalidOrderStatusException;
import com.example.ecommerce_system.exception.order.OrderDoesNotExist;
import com.example.ecommerce_system.exception.product.InsufficientProductStock;
import com.example.ecommerce_system.exception.product.ProductNotFoundException;
import com.example.ecommerce_system.model.*;
import com.example.ecommerce_system.store.CustomerStore;
import com.example.ecommerce_system.store.OrdersStore;
import com.example.ecommerce_system.store.ProductStore;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Service
public class OrderService {
    private OrdersStore orderStore;
    private CustomerStore customerStore;
    private ProductStore productStore;

    /**
     * Places a new order for the specified customer and returns the order response.
     */
    public OrderResponseDto placeOrder(OrderRequestDto request, UUID userId) {
        var customer = customerStore.getCustomerByUserId(userId).orElseThrow(
                () -> new CustomerNotFoundException(userId.toString()));

        var orderId = UUID.randomUUID();

        List<OrderItem> items = validateOrderItems(request.getItems(), orderId);
        double totalAmount = items.stream()
                .mapToDouble(item -> item.getPriceAtPurchase() * item.getQuantity())
                .sum();

        Orders newOrder = createOrder(orderId, request, customer.getCustomerId(), totalAmount);

        Orders savedOrder = orderStore.createOrder(newOrder, items);

        return map(savedOrder, items);
    }

    private List<OrderItem> validateOrderItems(List<OrderItemDto> orderedItems, UUID orderId) {
        return orderedItems.stream()
                .map(itemDto -> {
                    var productId = itemDto.getProductId();

                    Product product = productStore.getProduct(productId).orElseThrow(
                            () -> new ProductNotFoundException(productId.toString()));

                    if(product.getStockQuantity() < itemDto.getQuantity())
                        throw new InsufficientProductStock(productId.toString());

                    return OrderItem.builder()
                            .orderItemId(UUID.randomUUID())
                            .orderId(orderId)
                            .priceAtPurchase(product.getPrice())
                            .quantity(itemDto.getQuantity())
                            .productId(productId)
                            .build();

                }).toList();
    }

    private Orders createOrder(UUID orderId, OrderRequestDto request, UUID customerId, double totalAmount) {
        return Orders.builder()
                .orderId(orderId)
                .orderDate(Instant.now())
                .shippingCity(request.getCity())
                .shippingCountry(request.getCountry())
                .customerId(customerId)
                .shippingPostalCode(request.getPostalCode())
                .totalAmount(totalAmount)
                .status(OrderStatus.PENDING)
                .build();
    }

    private OrderResponseDto map(Orders order, List<OrderItem> items) {
        var itemsDto = items.stream().map(this::mapToOrderItemDto).toList();
        return OrderResponseDto.builder()
                .orderId(order.getOrderId())
                .items(itemsDto)
                .orderDate(order.getOrderDate())
                .status(order.getStatus())
                .shippingCity(order.getShippingCity())
                .shippingCountry(order.getShippingCountry())
                .shippingPostalCode(order.getShippingPostalCode())
                .totalAmount(order.getTotalAmount())
                .build();
    }

    private OrderItemDto mapToOrderItemDto(OrderItem item) {
        return OrderItemDto.builder()
                .orderItemId(item.getOrderItemId())
                .price(item.getPriceAtPurchase())
                .productId(item.getProductId())
                .quantity(item.getQuantity())
                .build();
    }

    /**
     * Retrieves an order and its items by order ID.
     */
    public OrderResponseDto getOrder(UUID orderId) {
        Orders order = orderStore.getOrder(orderId).orElseThrow(
                () -> new OrderDoesNotExist(orderId.toString()));
        List<OrderItem> items = orderStore.getOrderItemsByOrderId(orderId);

        return map(order, items);
    }

    /**
     * Retrieves all orders with pagination.
     */
    public List<OrderResponseDto> getAllOrders(int limit, int offset) {
        List<Orders> orders = orderStore.getAllOrders(limit, offset);
        return orders.stream()
                .map(order -> {
                    List<OrderItem> items = orderStore.getOrderItemsByOrderId(order.getOrderId());
                    return map(order, items);
                })
                .toList();
    }

    /**
     * Retrieves all orders for a customer with pagination.
     */
    public List<OrderResponseDto> getCustomerOrders(UUID customerId, int limit, int offset) {
        customerStore.getCustomer(customerId).orElseThrow(
                () -> new CustomerNotFoundException(customerId.toString()));

        List<Orders> orders = orderStore.getCustomerOrders(customerId, limit, offset);
        return orders.stream()
                .map(order -> {
                    List<OrderItem> items = orderStore.getOrderItemsByOrderId(order.getOrderId());
                    return map(order, items);
                })
                .toList();
    }

    /**
     * Updates the status of an order and returns the updated order response.
     */
    public OrderResponseDto updateOrderStatus(UUID orderId, OrderRequestDto request) {
        Orders existingOrder = orderStore.getOrder(orderId).orElseThrow(
                () -> new OrderDoesNotExist(orderId.toString()));

        Orders updatedOrder = switch (request.getStatus()) {
            case PROCESSED -> processOrder(existingOrder, orderId);
            case CANCELLED -> cancelOrder(existingOrder);
            default -> throw new InvalidOrderStatusException("this status is not allowed");
        };

        if (request.getStatus() == OrderStatus.CANCELLED) return buildOrderResponseWithoutItems(updatedOrder);

        List<OrderItem> items = orderStore.getOrderItemsByOrderId(orderId);
        return map(updatedOrder, items);
    }

    private Orders processOrder(Orders existingOrder, UUID orderId) {
        if (existingOrder.getStatus() == OrderStatus.PROCESSED) return existingOrder;

        List<OrderItem> items = orderStore.getOrderItemsByOrderId(orderId);
        List<UUID> productIds = items.stream().map(OrderItem::getProductId).toList();
        List<Integer> quantities = items.stream().map(OrderItem::getQuantity).toList();

        List<Integer> newStocks = validateAndCalculateNewStocks(productIds, quantities);

        productStore.updateProductStocks(productIds, newStocks);

        Orders processedOrder = buildOrderWithNewStatus(existingOrder, OrderStatus.PROCESSED);
        return orderStore.updateOrder(processedOrder);
    }

    private List<Integer> validateAndCalculateNewStocks(List<UUID> productIds, List<Integer> quantities) {
        List<Integer> newStocks = new java.util.ArrayList<>();

        for (int i = 0; i < productIds.size(); i++) {
            UUID productId = productIds.get(i);
            int quantityToDeduct = quantities.get(i);

            Product product = productStore.getProduct(productId)
                    .orElseThrow(() -> new ProductNotFoundException(productId.toString()));

            int newStock = product.getStockQuantity() - quantityToDeduct;
            if (newStock < 0) throw new InsufficientProductStock(productId.toString());

            newStocks.add(newStock);
        }

        return newStocks;
    }

    private Orders cancelOrder(Orders existingOrder) {
        if (existingOrder.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Only pending orders can be cancelled");
        }

        Orders cancelledOrder = buildOrderWithNewStatus(existingOrder, OrderStatus.CANCELLED);
        return orderStore.updateOrder(cancelledOrder);
    }

    private Orders buildOrderWithNewStatus(Orders existingOrder, OrderStatus newStatus) {
        return Orders.builder()
                .orderId(existingOrder.getOrderId())
                .customerId(existingOrder.getCustomerId())
                .orderDate(existingOrder.getOrderDate())
                .totalAmount(existingOrder.getTotalAmount())
                .shippingCountry(existingOrder.getShippingCountry())
                .shippingCity(existingOrder.getShippingCity())
                .shippingPostalCode(existingOrder.getShippingPostalCode())
                .status(newStatus)
                .build();
    }

    private OrderResponseDto buildOrderResponseWithoutItems(Orders order) {
        return OrderResponseDto.builder()
                .orderId(order.getOrderId())
                .orderDate(order.getOrderDate())
                .status(order.getStatus())
                .shippingCity(order.getShippingCity())
                .shippingCountry(order.getShippingCountry())
                .shippingPostalCode(order.getShippingPostalCode())
                .totalAmount(order.getTotalAmount())
                .items(null)
                .build();
    }
}

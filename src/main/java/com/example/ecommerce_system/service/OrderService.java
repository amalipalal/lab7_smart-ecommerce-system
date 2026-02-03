package com.example.ecommerce_system.service;

import com.example.ecommerce_system.dto.orders.OrderItemDto;
import com.example.ecommerce_system.dto.orders.OrderRequestDto;
import com.example.ecommerce_system.dto.orders.OrderResponseDto;
import com.example.ecommerce_system.exception.customer.CustomerNotFoundException;
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

    public OrderResponseDto placeOrder(OrderRequestDto request, UUID customerId) {
        Customer customer = customerStore.getCustomer(customerId).orElseThrow(
                () -> new CustomerNotFoundException(customerId.toString()));

        var orderId = UUID.randomUUID();

        List<OrderItem> items = validateOrderItems(request.getItems(), orderId);
        double totalAmount = items.stream().mapToDouble(OrderItem::getPriceAtPurchase).sum();

        Orders newOrder = createOrder(orderId, request, customerId, totalAmount);

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
                .items(itemsDto)
                .orderDate(order.getOrderDate())
                .status(order.getStatus())
                .shippingCity(order.getShippingCity())
                .shippingCountry(order.getShippingCountry())
                .shippingPostalCode(order.getShippingPostalCode())
                .orderId(order.getOrderId())
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

    public OrderResponseDto getOrder(UUID orderId) {
        Orders order = orderStore.getOrder(orderId).orElseThrow(
                () -> new OrderDoesNotExist(orderId.toString()));
        List<OrderItem> items = orderStore.getOrderItemsByOrderId(orderId);

        return map(order, items);
    }

    public List<OrderResponseDto> getAllOrders(int limit, int offset) {
        List<Orders> orders = orderStore.getAllOrders(limit, offset);
        return orders.stream()
                .map(order -> {
                    List<OrderItem> items = orderStore.getOrderItemsByOrderId(order.getOrderId());
                    return map(order, items);
                })
                .toList();
    }

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

    public OrderResponseDto updateOrderStatus(UUID orderId, OrderRequestDto request) {
        Orders existingOrder = orderStore.getOrder(orderId).orElseThrow(
                () -> new OrderDoesNotExist(orderId.toString()));

        Orders updatedOrder = Orders.builder()
                .orderId(existingOrder.getOrderId())
                .customerId(existingOrder.getCustomerId())
                .orderDate(existingOrder.getOrderDate())
                .totalAmount(existingOrder.getTotalAmount())
                .shippingCountry(existingOrder.getShippingCountry())
                .shippingCity(existingOrder.getShippingCity())
                .shippingPostalCode(existingOrder.getShippingPostalCode())
                .status(request.getStatus())
                .build();

        Orders savedOrder = orderStore.updateOrder(updatedOrder);
        List<OrderItem> items = orderStore.getOrderItemsByOrderId(orderId);

        return map(savedOrder, items);
    }
}

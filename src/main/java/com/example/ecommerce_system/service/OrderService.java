package com.example.ecommerce_system.service;

import com.example.ecommerce_system.dto.orders.OrderItemDto;
import com.example.ecommerce_system.dto.orders.OrderRequestDto;
import com.example.ecommerce_system.dto.orders.OrderResponseDto;
import com.example.ecommerce_system.model.OrderStatusType;
import com.example.ecommerce_system.exception.customer.CustomerNotFoundException;
import com.example.ecommerce_system.exception.order.InvalidOrderCancellationException;
import com.example.ecommerce_system.exception.order.InvalidOrderStatusException;
import com.example.ecommerce_system.exception.order.OrderDoesNotExist;
import com.example.ecommerce_system.exception.order.OrderStatusConfigurationException;
import com.example.ecommerce_system.exception.order.OrderStatusNotFoundException;
import com.example.ecommerce_system.exception.product.InsufficientProductStock;
import com.example.ecommerce_system.exception.product.ProductNotFoundException;
import com.example.ecommerce_system.model.*;
import com.example.ecommerce_system.repository.*;
import com.example.ecommerce_system.util.mapper.OrderMapper;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.example.ecommerce_system.model.OrderStatusType.CANCELLED;
import static com.example.ecommerce_system.model.OrderStatusType.PROCESSED;

@AllArgsConstructor
@Service
public class OrderService {

    private OrderRepository orderRepository;
    private OrderStatusRepository orderStatusRepository;
    private CustomerRepository customerRepository;
    private ProductRepository productRepository;

    private OrderMapper orderMapper;

    /**
     * Places a new order for the specified customer.
     * Validates order items, checks product availability and stock, calculates total amount,
     * and creates the order with PENDING status.
     */
    @Transactional
    public OrderResponseDto placeOrder(OrderRequestDto request, UUID userId) {
        var customer = checkIfCustomerExists(userId);
        var orderId = UUID.randomUUID();

        var status = orderStatusRepository.findOrderStatusByStatusName(OrderStatusType.PENDING)
                .orElseThrow(() -> new OrderStatusNotFoundException(OrderStatusType.PENDING.name()));

        List<OrderItem> items = validateOrderItems(request.getItems());
        double totalAmount = items.stream()
                .mapToDouble(item -> item.getPriceAtPurchase() * item.getQuantity())
                .sum();

        Orders newOrder = createOrder(orderId, request, customer, totalAmount, status);
        Orders savedOrder = orderRepository.save(newOrder);

        saveOrderItems(savedOrder, items);
        return orderMapper.toDto(savedOrder);
    }

    private void saveOrderItems(Orders savedOrder, List<OrderItem> items) {
        for (OrderItem item : items)
            item.setOrder(savedOrder);

        savedOrder.setOrderItems(items);
    }

    private Customer checkIfCustomerExists(UUID userId) {
        return customerRepository
                .findCustomerByUser_UserId(userId)
                .orElseThrow(() -> new CustomerNotFoundException(userId.toString()));
    }

    private List<OrderItem> validateOrderItems(List<OrderItemDto> orderedItems) {
        return orderedItems.stream()
                .map(itemDto -> {
                    var productId = itemDto.getProductId();
                    var product = productRepository.findById(productId)
                            .orElseThrow(() -> new ProductNotFoundException(productId.toString()));

                    if (product.getStockQuantity() < itemDto.getQuantity()) {
                        throw new InsufficientProductStock(productId.toString());
                    }

                    return OrderItem.builder()
                            .orderItemId(UUID.randomUUID())
                            .product(product)
                            .quantity(itemDto.getQuantity())
                            .priceAtPurchase(product.getPrice())
                            .build();
                })
                .toList();
    }

    private Orders createOrder(
            UUID orderId,
            OrderRequestDto request,
            Customer customer,
            double totalAmount,
            OrderStatus status
    ) {
        return Orders.builder()
                .orderId(orderId)
                .customer(customer)
                .orderDate(Instant.now())
                .shippingCity(request.getCity())
                .shippingCountry(request.getCountry())
                .shippingPostalCode(request.getPostalCode())
                .totalAmount(totalAmount)
                .status(status)
                .build();
    }

    /**
     * Retrieves an order and its items by order ID.
     */
    public OrderResponseDto getOrder(UUID orderId) {
        Orders order = orderRepository.findById(orderId).orElseThrow(
                () -> new OrderDoesNotExist(orderId.toString()));
        return orderMapper.toDto(order);
    }

    /**
     * Retrieves all orders with pagination.
     * Each order includes its associated items.
     */
    public List<OrderResponseDto> getAllOrders(int limit, int offset) {
        PageRequest pageRequest = PageRequest.of(
                offset,
                limit,
                Sort.by("orderDate").descending()
        );
        List<Orders> orders = orderRepository.findAll(pageRequest).getContent();
        return orderMapper.toDtoList(orders);
    }

    public List<OrderResponseDto> getCustomerOrders(UUID userId, int limit, int offset) {
        var customer = checkIfCustomerExists(userId);

        PageRequest pageRequest = PageRequest.of(
                offset,
                limit,
                Sort.by("orderDate").descending()
        );
        List<Orders> orders = orderRepository.findAllByCustomer_CustomerId(
                customer.getCustomerId(),
                pageRequest
        );
        return orderMapper.toDtoList(orders);
    }

    @Transactional
    public OrderResponseDto updateOrderStatus(UUID orderId, OrderRequestDto request) {
        Orders existingOrder = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderDoesNotExist(orderId.toString()));

        Orders updatedOrder = switch (request.getStatus()) {
            case PROCESSED -> processOrder(existingOrder);
            case CANCELLED -> cancelOrder(existingOrder);
            default -> throw new InvalidOrderStatusException("this status is not allowed");
        };

        return orderMapper.toDto(updatedOrder);
    }

    private Orders processOrder(Orders existingOrder) {
        if (existingOrder.getStatus().getStatusName() == PROCESSED)
            return existingOrder;

        for (OrderItem item : existingOrder.getOrderItems()) {
            Product product = item.getProduct();
            int newStock = product.getStockQuantity() - item.getQuantity();
            if (newStock < 0) throw new InsufficientProductStock(product.getProductId().toString());

            product.setStockQuantity(newStock);
            productRepository.save(product);
        }

        Orders processedOrder = buildOrderWithNewStatus(existingOrder, PROCESSED);
        return orderRepository.save(processedOrder);
    }

    private Orders cancelOrder(Orders existingOrder) {
        if (existingOrder.getStatus().getStatusName() != OrderStatusType.PENDING)
            throw new InvalidOrderCancellationException("Only pending orders can be cancelled");

        Orders cancelledOrder = buildOrderWithNewStatus(existingOrder, CANCELLED);
        return orderRepository.save(cancelledOrder);
    }

    private Orders buildOrderWithNewStatus(Orders existingOrder, OrderStatusType newStatus) {
        var status = orderStatusRepository.findOrderStatusByStatusName(newStatus)
                .orElseThrow(() -> new OrderStatusConfigurationException(newStatus.name()));

        return Orders.builder()
                .orderId(existingOrder.getOrderId())
                .customer(existingOrder.getCustomer())
                .orderDate(existingOrder.getOrderDate())
                .orderItems(existingOrder.getOrderItems())
                .totalAmount(existingOrder.getTotalAmount())
                .shippingCountry(existingOrder.getShippingCountry())
                .shippingCity(existingOrder.getShippingCity())
                .shippingPostalCode(existingOrder.getShippingPostalCode())
                .status(status)
                .build();
    }
}

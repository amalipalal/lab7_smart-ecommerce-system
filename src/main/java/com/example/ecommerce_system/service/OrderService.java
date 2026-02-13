package com.example.ecommerce_system.service;

import com.example.ecommerce_system.dto.orders.OrderFilter;
import com.example.ecommerce_system.dto.orders.OrderItemDto;
import com.example.ecommerce_system.dto.orders.OrderRequestDto;
import com.example.ecommerce_system.dto.orders.OrderResponseDto;
import com.example.ecommerce_system.model.OrderStatusType;
import com.example.ecommerce_system.exception.customer.CustomerNotFoundException;
import com.example.ecommerce_system.exception.order.InvalidOrderCancellationException;
import com.example.ecommerce_system.exception.order.InvalidOrderStatusException;
import com.example.ecommerce_system.exception.order.OrderDoesNotExist;
import com.example.ecommerce_system.exception.order.OrderStatusNotFoundException;
import com.example.ecommerce_system.exception.product.InsufficientProductStock;
import com.example.ecommerce_system.exception.product.ProductNotFoundException;
import com.example.ecommerce_system.exception.product.ProductOptimisticLockException;
import com.example.ecommerce_system.model.*;
import com.example.ecommerce_system.repository.*;
import com.example.ecommerce_system.util.OrderSpecification;
import com.example.ecommerce_system.util.mapper.OrderMapper;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.OptimisticLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

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
    @CacheEvict(value = {"orders", "paginated"}, allEntries = true)
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
    @Cacheable(value = "orders", key = "#orderId")
    public OrderResponseDto getOrder(UUID orderId) {
        Orders order = orderRepository.findById(orderId).orElseThrow(
                () -> new OrderDoesNotExist(orderId.toString()));
        return orderMapper.toDto(order);
    }

    /**
     * Retrieves all orders with pagination.
     */
    @Cacheable(value = "paginated", key = "'all_orders_' + #limit + '_' + #offset")
    public List<OrderResponseDto> getAllOrders(int limit, int offset) {
        PageRequest pageRequest = PageRequest.of(
                offset,
                limit,
                Sort.by("orderDate").descending()
        );
        List<Orders> orders = orderRepository.findAll(pageRequest).getContent();
        return orderMapper.toDtoList(orders);
    }

    /**
     * Searches orders using filter criteria with pagination.
     */
    @Cacheable(value = "paginated", key = "'search_orders_' + #filter.toString() + '_' + #limit + '_' + #offset")
    public List<OrderResponseDto> searchOrders(OrderFilter filter, int limit, int offset) {
        var orders = queryRepositoryWithFilter(filter, limit, offset);
        return orderMapper.toDtoList(orders);
    }

    private List<Orders> queryRepositoryWithFilter(OrderFilter filter, int limit, int offset) {
        Specification<Orders> spec = OrderSpecification.buildSpecification(filter);
        PageRequest pageRequest = PageRequest.of(
                offset,
                limit,
                Sort.by("orderDate").descending()
        );
        return orderRepository.findAll(spec, pageRequest).getContent();
    }

    /**
     * Retrieves all orders for a specific customer with pagination.
     */
    @Cacheable(value = "paginated", key = "'customer_orders_' + #userId + '_' + #limit + '_' + #offset")
    public List<OrderResponseDto> getCustomerOrders(UUID userId, int limit, int offset) {
        var customer = checkIfCustomerExists(userId);

        var filter = OrderFilter.builder()
                .customerId(customer.getCustomerId())
                .build();
        var orders = queryRepositoryWithFilter(filter, limit, offset);
        return orderMapper.toDtoList(orders);
    }

    /**
     * Updates order status to either PROCESSED or CANCELLED with retry mechanism.
     * Processing deducts stock quantities, cancellation is only allowed for pending orders.
     */
    @CacheEvict(value = {"orders", "products", "paginated"}, allEntries = true)
    @Transactional
    @Retryable(
        retryFor = {OptimisticLockException.class, ObjectOptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public OrderResponseDto updateOrderStatus(UUID orderId, OrderRequestDto request) {
        Orders existingOrder = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderDoesNotExist(orderId.toString()));

        switch (request.getStatus()) {
            case PROCESSED -> processOrderWithRetry(existingOrder);
            case CANCELLED -> cancelOrder(existingOrder);
            default -> throw new InvalidOrderStatusException("this status is not allowed");
        }

        return orderMapper.toDto(existingOrder);
    }

    @Retryable(
        retryFor = {OptimisticLockException.class, ObjectOptimisticLockingFailureException.class},
        maxAttempts = 5,
        backoff = @Backoff(delay = 50, multiplier = 1.5, maxDelay = 1000)
    )
    private void processOrderWithRetry(Orders existingOrder) {
        var orderId = existingOrder.getOrderId();
        try {
            processOrder(existingOrder);
        } catch (OptimisticLockException | ObjectOptimisticLockingFailureException e) {
            // Refresh the order and its items from database before retry
            existingOrder = orderRepository.findById(existingOrder.getOrderId())
                    .orElseThrow(() -> new OrderDoesNotExist(orderId.toString()));
            throw e;
        }
    }

    private void processOrder(Orders existingOrder) {
        if (existingOrder.getStatus().getStatusName() == PROCESSED)
            return;

        for (OrderItem item : existingOrder.getOrderItems()) {
            // Refresh product from database to get latest version
            var product = item.getProduct();

            int newStock = product.getStockQuantity() - item.getQuantity();
            if (newStock < 0)
                throw new InsufficientProductStock(product.getProductId().toString());

            product.setStockQuantity(newStock);
            product.setUpdatedAt(Instant.now());

            try {
                productRepository.save(product);
            } catch (OptimisticLockException | ObjectOptimisticLockingFailureException e) {
                throw new ProductOptimisticLockException(product.getProductId().toString(), e);
            }
        }

        var status = retrieveOrderStatus(PROCESSED);
        existingOrder.setStatus(status);
    }

    /**
     * Recover handlers used when optimistic lock keeps
     * occurring and proper error needs to be thrown.
     * Not explicitly called but referenced by retry context.
     */
    @Recover
    public OrderResponseDto recoverFromOptimisticLock(
            OptimisticLockException ex,
            UUID orderId,
            OrderRequestDto request) {
        throw new ProductOptimisticLockException("Failed to process order after multiple retries: " + orderId);
    }

    /**
     * Recover handlers used when optimistic lock keeps
     * occurring and proper error needs to be thrown.
     * Not explicitly called but referenced by retry context.
     */
    @Recover
    public OrderResponseDto recoverFromOptimisticLock(
            ObjectOptimisticLockingFailureException ex,
            UUID orderId,
            OrderRequestDto request) {
        throw new ProductOptimisticLockException("Failed to process order after multiple retries: " + orderId);
    }

    private void cancelOrder(Orders existingOrder) {
        if (existingOrder.getStatus().getStatusName() != OrderStatusType.PENDING)
            throw new InvalidOrderCancellationException("Only pending orders can be cancelled");

        var status = retrieveOrderStatus(CANCELLED);
        existingOrder.setStatus(status);
    }

    private OrderStatus retrieveOrderStatus(OrderStatusType type) {
        return orderStatusRepository.findOrderStatusByStatusName(type).orElseThrow();
    }
}

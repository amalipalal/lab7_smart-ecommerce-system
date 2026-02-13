package com.example.ecommerce_system;

import com.example.ecommerce_system.dto.orders.OrderFilter;
import com.example.ecommerce_system.dto.orders.OrderItemDto;
import com.example.ecommerce_system.dto.orders.OrderRequestDto;
import com.example.ecommerce_system.dto.orders.OrderResponseDto;
import com.example.ecommerce_system.model.OrderStatusType;
import com.example.ecommerce_system.exception.customer.CustomerNotFoundException;
import com.example.ecommerce_system.exception.order.InvalidOrderCancellationException;
import com.example.ecommerce_system.exception.order.InvalidOrderStatusException;
import com.example.ecommerce_system.exception.order.OrderDoesNotExist;
import com.example.ecommerce_system.exception.product.InsufficientProductStock;
import com.example.ecommerce_system.exception.product.ProductNotFoundException;
import com.example.ecommerce_system.model.*;
import com.example.ecommerce_system.repository.*;
import com.example.ecommerce_system.service.OrderService;
import com.example.ecommerce_system.util.mapper.OrderMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderStatusRepository orderStatusRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderService orderService;

    private OrderStatus pendingStatus;
    private OrderStatus processedStatus;
    private OrderStatus cancelledStatus;

    @BeforeEach
    void setUp() {
        pendingStatus = OrderStatus.builder()
                .statusId(UUID.randomUUID())
                .statusName(OrderStatusType.PENDING)
                .description("Order is pending")
                .build();

        processedStatus = OrderStatus.builder()
                .statusId(UUID.randomUUID())
                .statusName(OrderStatusType.PROCESSED)
                .description("Order has been processed")
                .build();

        cancelledStatus = OrderStatus.builder()
                .statusId(UUID.randomUUID())
                .statusName(OrderStatusType.CANCELLED)
                .description("Order has been cancelled")
                .build();
    }

    @Test
    @DisplayName("Should place order successfully")
    void shouldPlaceOrderSuccessfully() {
        UUID userId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        User user = User.builder()
                .userId(userId)
                .email("test@example.com")
                .build();

        Customer customer = Customer.builder()
                .customerId(customerId)
                .firstName("John")
                .lastName("Doe")
                .user(user)
                .build();

        Product product = Product.builder()
                .productId(productId)
                .name("Laptop")
                .price(1200.0)
                .stockQuantity(10)
                .build();

        OrderItemDto itemDto = OrderItemDto.builder()
                .productId(productId)
                .quantity(2)
                .build();

        OrderRequestDto request = OrderRequestDto.builder()
                .items(List.of(itemDto))
                .city("Accra")
                .country("Ghana")
                .postalCode("00233")
                .build();

        Orders savedOrder = Orders.builder()
                .orderId(UUID.randomUUID())
                .customer(customer)
                .totalAmount(2400.0)
                .status(pendingStatus)
                .orderDate(Instant.now())
                .shippingCity("Accra")
                .shippingCountry("Ghana")
                .shippingPostalCode("00233")
                .orderItems(new ArrayList<>())
                .build();

        OrderResponseDto responseDto = OrderResponseDto.builder()
                .orderId(savedOrder.getOrderId())
                .status(OrderStatusType.PENDING.name())
                .totalAmount(2400.0)
                .build();

        when(customerRepository.findCustomerByUser_UserId(userId)).thenReturn(Optional.of(customer));
        when(orderStatusRepository.findOrderStatusByStatusName(OrderStatusType.PENDING))
                .thenReturn(Optional.of(pendingStatus));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Orders.class))).thenReturn(savedOrder);
        when(orderMapper.toDto(any(Orders.class))).thenReturn(responseDto);

        OrderResponseDto response = orderService.placeOrder(request, userId);

        Assertions.assertNotNull(response.getOrderId());
        Assertions.assertEquals(OrderStatusType.PENDING.name(), response.getStatus());
        verify(customerRepository).findCustomerByUser_UserId(userId);
        verify(productRepository).findById(productId);
        verify(orderRepository).save(any(Orders.class));
    }

    @Test
    @DisplayName("Should throw error when placing order for non-existing customer")
    void shouldThrowWhenPlacingOrderForNonExistingCustomer() {
        UUID userId = UUID.randomUUID();
        OrderRequestDto request = OrderRequestDto.builder()
                .items(List.of())
                .build();

        when(customerRepository.findCustomerByUser_UserId(userId)).thenReturn(Optional.empty());

        Assertions.assertThrows(
                CustomerNotFoundException.class,
                () -> orderService.placeOrder(request, userId)
        );

        verify(customerRepository).findCustomerByUser_UserId(userId);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw error when product not found")
    void shouldThrowWhenProductNotFound() {
        UUID userId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        User user = User.builder()
                .userId(userId)
                .build();

        Customer customer = Customer.builder()
                .customerId(customerId)
                .user(user)
                .build();

        OrderItemDto itemDto = OrderItemDto.builder()
                .productId(productId)
                .quantity(1)
                .build();

        OrderRequestDto request = OrderRequestDto.builder()
                .items(List.of(itemDto))
                .city("Accra")
                .country("Ghana")
                .postalCode("00233")
                .build();

        when(customerRepository.findCustomerByUser_UserId(userId)).thenReturn(Optional.of(customer));
        when(orderStatusRepository.findOrderStatusByStatusName(OrderStatusType.PENDING))
                .thenReturn(Optional.of(pendingStatus));
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        Assertions.assertThrows(
                ProductNotFoundException.class,
                () -> orderService.placeOrder(request, userId)
        );

        verify(productRepository).findById(productId);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw error when insufficient product stock")
    void shouldThrowWhenInsufficientStock() {
        UUID userId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        User user = User.builder()
                .userId(userId)
                .build();

        Customer customer = Customer.builder()
                .customerId(customerId)
                .user(user)
                .build();

        Product product = Product.builder()
                .productId(productId)
                .stockQuantity(2)
                .build();

        OrderItemDto itemDto = OrderItemDto.builder()
                .productId(productId)
                .quantity(5)
                .build();

        OrderRequestDto request = OrderRequestDto.builder()
                .items(List.of(itemDto))
                .city("Accra")
                .country("Ghana")
                .postalCode("00233")
                .build();

        when(customerRepository.findCustomerByUser_UserId(userId)).thenReturn(Optional.of(customer));
        when(orderStatusRepository.findOrderStatusByStatusName(OrderStatusType.PENDING))
                .thenReturn(Optional.of(pendingStatus));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        Assertions.assertThrows(
                InsufficientProductStock.class,
                () -> orderService.placeOrder(request, userId)
        );

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get order by id successfully")
    void shouldGetOrderByIdSuccessfully() {
        UUID orderId = UUID.randomUUID();
        Orders order = Orders.builder()
                .orderId(orderId)
                .customer(Customer.builder().customerId(UUID.randomUUID()).build())
                .totalAmount(1200.0)
                .status(pendingStatus)
                .orderDate(Instant.now())
                .orderItems(new ArrayList<>())
                .build();

        OrderResponseDto responseDto = OrderResponseDto.builder()
                .orderId(orderId)
                .status(OrderStatusType.PENDING.name())
                .totalAmount(1200.0)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderMapper.toDto(order)).thenReturn(responseDto);

        OrderResponseDto response = orderService.getOrder(orderId);

        Assertions.assertEquals(orderId, response.getOrderId());
        verify(orderRepository).findById(orderId);
        verify(orderMapper).toDto(order);
    }

    @Test
    @DisplayName("Should throw error when order not found")
    void shouldThrowWhenOrderNotFound() {
        UUID orderId = UUID.randomUUID();

        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        Assertions.assertThrows(
                OrderDoesNotExist.class,
                () -> orderService.getOrder(orderId)
        );

        verify(orderRepository).findById(orderId);
    }

    @Test
    @DisplayName("Should get all orders successfully")
    void shouldGetAllOrdersSuccessfully() {
        Orders order1 = Orders.builder()
                .orderId(UUID.randomUUID())
                .customer(Customer.builder().customerId(UUID.randomUUID()).build())
                .totalAmount(1200.0)
                .status(pendingStatus)
                .orderDate(Instant.now())
                .orderItems(new ArrayList<>())
                .build();

        Orders order2 = Orders.builder()
                .orderId(UUID.randomUUID())
                .customer(Customer.builder().customerId(UUID.randomUUID()).build())
                .totalAmount(800.0)
                .status(processedStatus)
                .orderDate(Instant.now())
                .orderItems(new ArrayList<>())
                .build();

        Page<Orders> ordersPage = new PageImpl<>(List.of(order1, order2));
        List<OrderResponseDto> responseDtos = List.of(
                OrderResponseDto.builder().orderId(order1.getOrderId()).build(),
                OrderResponseDto.builder().orderId(order2.getOrderId()).build()
        );

        when(orderRepository.findAll(any(PageRequest.class))).thenReturn(ordersPage);
        when(orderMapper.toDtoList(anyList())).thenReturn(responseDtos);

        List<OrderResponseDto> response = orderService.getAllOrders(10, 0);

        Assertions.assertEquals(2, response.size());
        verify(orderRepository).findAll(any(PageRequest.class));
    }

    @Test
    @DisplayName("Should get customer orders successfully")
    @SuppressWarnings("unchecked")
    void shouldGetCustomerOrdersSuccessfully() {
        UUID userId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        User user = User.builder()
                .userId(userId)
                .build();

        Customer customer = Customer.builder()
                .customerId(customerId)
                .user(user)
                .build();

        Orders order = Orders.builder()
                .orderId(UUID.randomUUID())
                .customer(customer)
                .totalAmount(1200.0)
                .status(pendingStatus)
                .orderDate(Instant.now())
                .orderItems(new ArrayList<>())
                .build();

        List<OrderResponseDto> responseDtos = List.of(
                OrderResponseDto.builder().orderId(order.getOrderId()).build()
        );

        Page<Orders> ordersPage = new PageImpl<>(List.of(order), PageRequest.of(10, 5), 0);

        when(customerRepository.findCustomerByUser_UserId(userId)).thenReturn(Optional.of(customer));
        when(orderRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(ordersPage);
        when(orderMapper.toDtoList(anyList())).thenReturn(responseDtos);

        List<OrderResponseDto> response = orderService.getCustomerOrders(userId, 10, 0);

        Assertions.assertEquals(1, response.size());
        verify(customerRepository).findCustomerByUser_UserId(userId);
        verify(orderRepository).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    @DisplayName("Should throw error when getting orders for non-existing customer")
    void shouldThrowWhenGettingOrdersForNonExistingCustomer() {
        UUID userId = UUID.randomUUID();

        when(customerRepository.findCustomerByUser_UserId(userId)).thenReturn(Optional.empty());

        Assertions.assertThrows(
                CustomerNotFoundException.class,
                () -> orderService.getCustomerOrders(userId, 10, 0)
        );

        verify(customerRepository).findCustomerByUser_UserId(userId);
        verify(orderRepository, never()).findAllByCustomer_CustomerId(any(), any());
    }

    @Test
    @DisplayName("Should handle pagination in get all orders")
    void shouldHandlePaginationInGetAllOrders() {
        Page<Orders> emptyPage = new PageImpl<>(List.of());

        when(orderRepository.findAll(any(PageRequest.class))).thenReturn(emptyPage);
        when(orderMapper.toDtoList(anyList())).thenReturn(List.of());

        List<OrderResponseDto> response = orderService.getAllOrders(5, 10);

        Assertions.assertEquals(0, response.size());
        verify(orderRepository).findAll(any(PageRequest.class));
    }

    @Test
    @DisplayName("Should handle pagination in get customer orders")
    @SuppressWarnings("unchecked")
    void shouldHandlePaginationInGetCustomerOrders() {
        UUID userId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        User user = User.builder()
                .userId(userId)
                .build();

        Customer customer = Customer.builder()
                .customerId(customerId)
                .user(user)
                .build();

        Page<Orders> emptyPage = new PageImpl<>(List.of());

        when(customerRepository.findCustomerByUser_UserId(userId)).thenReturn(Optional.of(customer));
        when(orderRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(emptyPage);
        when(orderMapper.toDtoList(anyList())).thenReturn(List.of());

        List<OrderResponseDto> response = orderService.getCustomerOrders(userId, 5, 10);

        Assertions.assertEquals(0, response.size());
        verify(orderRepository).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    @DisplayName("Should calculate total amount correctly for multiple items")
    void shouldCalculateTotalAmountForMultipleItems() {
        UUID userId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();

        User user = User.builder()
                .userId(userId)
                .build();

        Customer customer = Customer.builder()
                .customerId(customerId)
                .user(user)
                .build();

        Product product1 = Product.builder()
                .productId(productId1)
                .price(500.0)
                .stockQuantity(10)
                .build();

        Product product2 = Product.builder()
                .productId(productId2)
                .price(300.0)
                .stockQuantity(10)
                .build();

        OrderItemDto itemDto1 = OrderItemDto.builder()
                .productId(productId1)
                .quantity(2)
                .build();

        OrderItemDto itemDto2 = OrderItemDto.builder()
                .productId(productId2)
                .quantity(3)
                .build();

        OrderRequestDto request = OrderRequestDto.builder()
                .items(List.of(itemDto1, itemDto2))
                .city("Accra")
                .country("Ghana")
                .postalCode("00233")
                .build();

        Orders savedOrder = Orders.builder()
                .orderId(UUID.randomUUID())
                .customer(customer)
                .totalAmount(1900.0)
                .status(pendingStatus)
                .orderDate(Instant.now())
                .shippingCity("Accra")
                .shippingCountry("Ghana")
                .shippingPostalCode("00233")
                .orderItems(new ArrayList<>())
                .build();

        OrderResponseDto responseDto = OrderResponseDto.builder()
                .orderId(savedOrder.getOrderId())
                .status(OrderStatusType.PENDING.name())
                .totalAmount(1900.0)
                .build();

        when(customerRepository.findCustomerByUser_UserId(userId)).thenReturn(Optional.of(customer));
        when(orderStatusRepository.findOrderStatusByStatusName(OrderStatusType.PENDING))
                .thenReturn(Optional.of(pendingStatus));
        when(productRepository.findById(productId1)).thenReturn(Optional.of(product1));
        when(productRepository.findById(productId2)).thenReturn(Optional.of(product2));
        when(orderRepository.save(any(Orders.class))).thenReturn(savedOrder);
        when(orderMapper.toDto(any(Orders.class))).thenReturn(responseDto);

        OrderResponseDto response = orderService.placeOrder(request, userId);

        Assertions.assertNotNull(response);
        verify(orderRepository).save(any(Orders.class));
    }

    @Test
    @DisplayName("Should update order status to CANCELLED successfully")
    void shouldUpdateOrderStatusToCancelled() {
        UUID orderId = UUID.randomUUID();

        Orders existingOrder = Orders.builder()
                .orderId(orderId)
                .customer(Customer.builder().customerId(UUID.randomUUID()).build())
                .totalAmount(1200.0)
                .status(pendingStatus)
                .orderDate(Instant.now())
                .orderItems(new ArrayList<>())
                .build();

        OrderResponseDto responseDto = OrderResponseDto.builder()
                .orderId(orderId)
                .status(OrderStatusType.CANCELLED.name())
                .totalAmount(1200.0)
                .items(null)
                .build();

        OrderRequestDto request = OrderRequestDto.builder()
                .status(OrderStatusType.CANCELLED)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));
        when(orderStatusRepository.findOrderStatusByStatusName(OrderStatusType.CANCELLED))
                .thenReturn(Optional.of(cancelledStatus));
        when(orderMapper.toDto(any(Orders.class))).thenReturn(responseDto);

        OrderResponseDto response = orderService.updateOrderStatus(orderId, request);

        Assertions.assertEquals(OrderStatusType.CANCELLED.name(), response.getStatus());
        Assertions.assertEquals(cancelledStatus, existingOrder.getStatus());
        verify(orderRepository).findById(orderId);
        verify(orderStatusRepository).findOrderStatusByStatusName(OrderStatusType.CANCELLED);
        verify(orderMapper).toDto(existingOrder);
    }

    @Test
    @DisplayName("Should update order status to PROCESSED successfully")
    void shouldUpdateOrderStatusToProcessed() {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        Product product = Product.builder()
                .productId(productId)
                .stockQuantity(10)
                .build();

        OrderItem item = OrderItem.builder()
                .orderItemId(UUID.randomUUID())
                .product(product)
                .quantity(2)
                .priceAtPurchase(600.0)
                .build();

        Orders existingOrder = Orders.builder()
                .orderId(orderId)
                .customer(Customer.builder().customerId(UUID.randomUUID()).build())
                .totalAmount(1200.0)
                .status(pendingStatus)
                .orderDate(Instant.now())
                .orderItems(List.of(item))
                .build();

        OrderResponseDto responseDto = OrderResponseDto.builder()
                .orderId(orderId)
                .status(OrderStatusType.PROCESSED.name())
                .totalAmount(1200.0)
                .build();

        OrderRequestDto request = OrderRequestDto.builder()
                .status(OrderStatusType.PROCESSED)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));
        when(orderStatusRepository.findOrderStatusByStatusName(OrderStatusType.PROCESSED))
                .thenReturn(Optional.of(processedStatus));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(orderMapper.toDto(any(Orders.class))).thenReturn(responseDto);

        OrderResponseDto response = orderService.updateOrderStatus(orderId, request);

        Assertions.assertEquals(OrderStatusType.PROCESSED.name(), response.getStatus());
        Assertions.assertEquals(processedStatus, existingOrder.getStatus());
        Assertions.assertEquals(8, product.getStockQuantity());
        verify(orderRepository).findById(orderId);
        verify(orderStatusRepository).findOrderStatusByStatusName(OrderStatusType.PROCESSED);
        verify(productRepository).save(product);
        verify(orderMapper).toDto(existingOrder);
    }

    @Test
    @DisplayName("Should throw error when cancelling non-pending order")
    void shouldThrowWhenCancellingNonPendingOrder() {
        UUID orderId = UUID.randomUUID();

        Orders existingOrder = Orders.builder()
                .orderId(orderId)
                .status(processedStatus)
                .orderItems(new ArrayList<>())
                .build();

        OrderRequestDto request = OrderRequestDto.builder()
                .status(OrderStatusType.CANCELLED)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));

        Assertions.assertThrows(
                InvalidOrderCancellationException.class,
                () -> orderService.updateOrderStatus(orderId, request)
        );

        verify(orderRepository).findById(orderId);
        verify(orderStatusRepository, never()).findOrderStatusByStatusName(any());
    }

    @Test
    @DisplayName("Should throw error when updating with invalid status")
    void shouldThrowWhenUpdatingWithInvalidStatus() {
        UUID orderId = UUID.randomUUID();

        Orders existingOrder = Orders.builder()
                .orderId(orderId)
                .status(pendingStatus)
                .orderItems(new ArrayList<>())
                .build();

        OrderRequestDto request = OrderRequestDto.builder()
                .status(OrderStatusType.PENDING)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));

        Assertions.assertThrows(
                InvalidOrderStatusException.class,
                () -> orderService.updateOrderStatus(orderId, request)
        );

        verify(orderRepository).findById(orderId);
        verify(orderStatusRepository, never()).findOrderStatusByStatusName(any());
    }

    @Test
    @DisplayName("Should throw error when processing order with insufficient stock")
    void shouldThrowWhenProcessingOrderWithInsufficientStock() {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        Product product = Product.builder()
                .productId(productId)
                .stockQuantity(5)
                .build();

        OrderItem item = OrderItem.builder()
                .product(product)
                .quantity(10)
                .build();

        Orders existingOrder = Orders.builder()
                .orderId(orderId)
                .status(pendingStatus)
                .orderItems(List.of(item))
                .build();

        OrderRequestDto request = OrderRequestDto.builder()
                .status(OrderStatusType.PROCESSED)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));

        Assertions.assertThrows(
                InsufficientProductStock.class,
                () -> orderService.updateOrderStatus(orderId, request)
        );

        verify(orderRepository).findById(orderId);
    }

    @Test
    @DisplayName("Should search orders with filter successfully")
    @SuppressWarnings("unchecked")
    void shouldSearchOrdersWithFilterSuccessfully() {
        UUID customerId = UUID.randomUUID();
        OrderFilter filter = OrderFilter.builder()
                .customerId(customerId)
                .status(OrderStatusType.PENDING)
                .build();

        Customer customer = Customer.builder()
                .customerId(customerId)
                .firstName("John")
                .lastName("Doe")
                .build();

        Orders order = Orders.builder()
                .orderId(UUID.randomUUID())
                .customer(customer)
                .totalAmount(1200.0)
                .status(pendingStatus)
                .orderDate(Instant.now())
                .orderItems(new ArrayList<>())
                .build();

        Page<Orders> ordersPage = new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1);
        List<OrderResponseDto> responseDtos = List.of(
                OrderResponseDto.builder()
                        .orderId(order.getOrderId())
                        .status(OrderStatusType.PENDING.name())
                        .totalAmount(1200.0)
                        .build()
        );

        when(orderRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(ordersPage);
        when(orderMapper.toDtoList(anyList())).thenReturn(responseDtos);

        List<OrderResponseDto> response = orderService.searchOrders(filter, 10, 0);

        Assertions.assertEquals(1, response.size());
        Assertions.assertEquals(OrderStatusType.PENDING.name(), response.get(0).getStatus());
        verify(orderRepository).findAll(any(Specification.class), any(PageRequest.class));
        verify(orderMapper).toDtoList(anyList());
    }

    @Test
    @DisplayName("Should return empty list when no orders match filter")
    @SuppressWarnings("unchecked")
    void shouldReturnEmptyListWhenNoOrdersMatchFilter() {
        OrderFilter filter = OrderFilter.builder()
                .status(OrderStatusType.CANCELLED)
                .build();

        Page<Orders> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(orderRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(emptyPage);
        when(orderMapper.toDtoList(anyList())).thenReturn(List.of());

        List<OrderResponseDto> response = orderService.searchOrders(filter, 10, 0);

        Assertions.assertEquals(0, response.size());
        verify(orderRepository).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    @DisplayName("Should handle pagination correctly in search")
    @SuppressWarnings("unchecked")
    void shouldHandlePaginationInSearch() {
        OrderFilter filter = OrderFilter.builder()
                .status(OrderStatusType.PROCESSED)
                .build();

        Orders order = Orders.builder()
                .orderId(UUID.randomUUID())
                .customer(Customer.builder().customerId(UUID.randomUUID()).build())
                .totalAmount(800.0)
                .status(processedStatus)
                .orderDate(Instant.now())
                .orderItems(new ArrayList<>())
                .build();

        Page<Orders> ordersPage = new PageImpl<>(List.of(order), PageRequest.of(5, 5), 1);
        List<OrderResponseDto> responseDtos = List.of(
                OrderResponseDto.builder()
                        .orderId(order.getOrderId())
                        .status(OrderStatusType.PROCESSED.name())
                        .build()
        );

        when(orderRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(ordersPage);
        when(orderMapper.toDtoList(anyList())).thenReturn(responseDtos);

        List<OrderResponseDto> response = orderService.searchOrders(filter, 5, 5);

        Assertions.assertEquals(1, response.size());
        verify(orderRepository).findAll(any(Specification.class), eq(PageRequest.of(5, 5, org.springframework.data.domain.Sort.by("orderDate").descending())));
    }
}

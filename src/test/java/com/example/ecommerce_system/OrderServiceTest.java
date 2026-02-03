package com.example.ecommerce_system;

import com.example.ecommerce_system.dto.orders.OrderItemDto;
import com.example.ecommerce_system.dto.orders.OrderRequestDto;
import com.example.ecommerce_system.dto.orders.OrderResponseDto;
import com.example.ecommerce_system.exception.customer.CustomerNotFoundException;
import com.example.ecommerce_system.exception.order.OrderDoesNotExist;
import com.example.ecommerce_system.exception.product.InsufficientProductStock;
import com.example.ecommerce_system.exception.product.ProductNotFoundException;
import com.example.ecommerce_system.model.*;
import com.example.ecommerce_system.service.OrderService;
import com.example.ecommerce_system.store.CustomerStore;
import com.example.ecommerce_system.store.OrdersStore;
import com.example.ecommerce_system.store.ProductStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrdersStore orderStore;

    @Mock
    private CustomerStore customerStore;

    @Mock
    private ProductStore productStore;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("Should place order successfully")
    void shouldPlaceOrderSuccessfully() {
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        Customer customer = Customer.builder()
                .customerId(customerId)
                .firstName("John")
                .lastName("Doe")
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
                .customerId(customerId)
                .totalAmount(2400.0)
                .status(OrderStatus.PENDING)
                .orderDate(Instant.now())
                .shippingCity("Accra")
                .shippingCountry("Ghana")
                .shippingPostalCode("00233")
                .build();

        when(customerStore.getCustomer(customerId)).thenReturn(Optional.of(customer));
        when(productStore.getProduct(productId)).thenReturn(Optional.of(product));
        when(orderStore.createOrder(any(Orders.class), anyList())).thenReturn(savedOrder);

        OrderResponseDto response = orderService.placeOrder(request, customerId);

        Assertions.assertNotNull(response.getOrderId());
        Assertions.assertEquals(OrderStatus.PENDING, response.getStatus());
        Assertions.assertEquals(1, response.getItems().size());
        verify(customerStore).getCustomer(customerId);
        verify(productStore).getProduct(productId);
        verify(orderStore).createOrder(any(Orders.class), anyList());
    }

    @Test
    @DisplayName("Should throw error when placing order for non-existing customer")
    void shouldThrowWhenPlacingOrderForNonExistingCustomer() {
        UUID customerId = UUID.randomUUID();
        OrderRequestDto request = OrderRequestDto.builder()
                .items(List.of())
                .build();

        when(customerStore.getCustomer(customerId)).thenReturn(Optional.empty());

        Assertions.assertThrows(
                CustomerNotFoundException.class,
                () -> orderService.placeOrder(request, customerId)
        );

        verify(customerStore).getCustomer(customerId);
        verify(orderStore, never()).createOrder(any(), anyList());
    }

    @Test
    @DisplayName("Should throw error when product not found")
    void shouldThrowWhenProductNotFound() {
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        Customer customer = Customer.builder()
                .customerId(customerId)
                .build();

        OrderItemDto itemDto = OrderItemDto.builder()
                .productId(productId)
                .quantity(1)
                .build();

        OrderRequestDto request = OrderRequestDto.builder()
                .items(List.of(itemDto))
                .build();

        when(customerStore.getCustomer(customerId)).thenReturn(Optional.of(customer));
        when(productStore.getProduct(productId)).thenReturn(Optional.empty());

        Assertions.assertThrows(
                ProductNotFoundException.class,
                () -> orderService.placeOrder(request, customerId)
        );

        verify(productStore).getProduct(productId);
        verify(orderStore, never()).createOrder(any(), anyList());
    }

    @Test
    @DisplayName("Should throw error when insufficient product stock")
    void shouldThrowWhenInsufficientStock() {
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        Customer customer = Customer.builder()
                .customerId(customerId)
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
                .build();

        when(customerStore.getCustomer(customerId)).thenReturn(Optional.of(customer));
        when(productStore.getProduct(productId)).thenReturn(Optional.of(product));

        Assertions.assertThrows(
                InsufficientProductStock.class,
                () -> orderService.placeOrder(request, customerId)
        );

        verify(orderStore, never()).createOrder(any(), anyList());
    }

    @Test
    @DisplayName("Should get order by id successfully")
    void shouldGetOrderByIdSuccessfully() {
        UUID orderId = UUID.randomUUID();
        Orders order = Orders.builder()
                .orderId(orderId)
                .customerId(UUID.randomUUID())
                .totalAmount(1200.0)
                .status(OrderStatus.PENDING)
                .orderDate(Instant.now())
                .build();

        OrderItem item = OrderItem.builder()
                .orderItemId(UUID.randomUUID())
                .orderId(orderId)
                .productId(UUID.randomUUID())
                .quantity(1)
                .priceAtPurchase(1200.0)
                .build();

        when(orderStore.getOrder(orderId)).thenReturn(Optional.of(order));
        when(orderStore.getOrderItemsByOrderId(orderId)).thenReturn(List.of(item));

        OrderResponseDto response = orderService.getOrder(orderId);

        Assertions.assertEquals(orderId, response.getOrderId());
        Assertions.assertEquals(1, response.getItems().size());
        verify(orderStore).getOrder(orderId);
        verify(orderStore).getOrderItemsByOrderId(orderId);
    }

    @Test
    @DisplayName("Should throw error when order not found")
    void shouldThrowWhenOrderNotFound() {
        UUID orderId = UUID.randomUUID();

        when(orderStore.getOrder(orderId)).thenReturn(Optional.empty());

        Assertions.assertThrows(
                OrderDoesNotExist.class,
                () -> orderService.getOrder(orderId)
        );

        verify(orderStore).getOrder(orderId);
        verify(orderStore, never()).getOrderItemsByOrderId(any());
    }

    @Test
    @DisplayName("Should get all orders successfully")
    void shouldGetAllOrdersSuccessfully() {
        Orders order1 = Orders.builder()
                .orderId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .totalAmount(1200.0)
                .status(OrderStatus.PENDING)
                .orderDate(Instant.now())
                .build();

        Orders order2 = Orders.builder()
                .orderId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .totalAmount(800.0)
                .status(OrderStatus.PROCESSED)
                .orderDate(Instant.now())
                .build();

        OrderItem item1 = OrderItem.builder()
                .orderItemId(UUID.randomUUID())
                .orderId(order1.getOrderId())
                .productId(UUID.randomUUID())
                .quantity(1)
                .priceAtPurchase(1200.0)
                .build();

        OrderItem item2 = OrderItem.builder()
                .orderItemId(UUID.randomUUID())
                .orderId(order2.getOrderId())
                .productId(UUID.randomUUID())
                .quantity(2)
                .priceAtPurchase(400.0)
                .build();

        when(orderStore.getAllOrders(10, 0)).thenReturn(List.of(order1, order2));
        when(orderStore.getOrderItemsByOrderId(order1.getOrderId())).thenReturn(List.of(item1));
        when(orderStore.getOrderItemsByOrderId(order2.getOrderId())).thenReturn(List.of(item2));

        List<OrderResponseDto> response = orderService.getAllOrders(10, 0);

        Assertions.assertEquals(2, response.size());
        verify(orderStore).getAllOrders(10, 0);
        verify(orderStore, times(2)).getOrderItemsByOrderId(any());
    }

    @Test
    @DisplayName("Should get customer orders successfully")
    void shouldGetCustomerOrdersSuccessfully() {
        UUID customerId = UUID.randomUUID();
        Customer customer = Customer.builder()
                .customerId(customerId)
                .build();

        Orders order = Orders.builder()
                .orderId(UUID.randomUUID())
                .customerId(customerId)
                .totalAmount(1200.0)
                .status(OrderStatus.PENDING)
                .orderDate(Instant.now())
                .build();

        OrderItem item = OrderItem.builder()
                .orderItemId(UUID.randomUUID())
                .orderId(order.getOrderId())
                .productId(UUID.randomUUID())
                .quantity(1)
                .priceAtPurchase(1200.0)
                .build();

        when(customerStore.getCustomer(customerId)).thenReturn(Optional.of(customer));
        when(orderStore.getCustomerOrders(customerId, 10, 0)).thenReturn(List.of(order));
        when(orderStore.getOrderItemsByOrderId(order.getOrderId())).thenReturn(List.of(item));

        List<OrderResponseDto> response = orderService.getCustomerOrders(customerId, 10, 0);

        Assertions.assertEquals(1, response.size());
        Assertions.assertEquals(order.getOrderId(), response.get(0).getOrderId());
        verify(customerStore).getCustomer(customerId);
        verify(orderStore).getCustomerOrders(customerId, 10, 0);
    }

    @Test
    @DisplayName("Should throw error when getting orders for non-existing customer")
    void shouldThrowWhenGettingOrdersForNonExistingCustomer() {
        UUID customerId = UUID.randomUUID();

        when(customerStore.getCustomer(customerId)).thenReturn(Optional.empty());

        Assertions.assertThrows(
                CustomerNotFoundException.class,
                () -> orderService.getCustomerOrders(customerId, 10, 0)
        );

        verify(customerStore).getCustomer(customerId);
        verify(orderStore, never()).getCustomerOrders(any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("Should handle pagination in get all orders")
    void shouldHandlePaginationInGetAllOrders() {
        when(orderStore.getAllOrders(5, 10)).thenReturn(List.of());

        List<OrderResponseDto> response = orderService.getAllOrders(5, 10);

        Assertions.assertEquals(0, response.size());
        verify(orderStore).getAllOrders(5, 10);
    }

    @Test
    @DisplayName("Should handle pagination in get customer orders")
    void shouldHandlePaginationInGetCustomerOrders() {
        UUID customerId = UUID.randomUUID();
        Customer customer = Customer.builder()
                .customerId(customerId)
                .build();

        when(customerStore.getCustomer(customerId)).thenReturn(Optional.of(customer));
        when(orderStore.getCustomerOrders(customerId, 5, 10)).thenReturn(List.of());

        List<OrderResponseDto> response = orderService.getCustomerOrders(customerId, 5, 10);

        Assertions.assertEquals(0, response.size());
        verify(orderStore).getCustomerOrders(customerId, 5, 10);
    }

    @Test
    @DisplayName("Should calculate total amount correctly for multiple items")
    void shouldCalculateTotalAmountForMultipleItems() {
        UUID customerId = UUID.randomUUID();
        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();

        Customer customer = Customer.builder()
                .customerId(customerId)
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
                .customerId(customerId)
                .totalAmount(1000.0)
                .status(OrderStatus.PENDING)
                .orderDate(Instant.now())
                .shippingCity("Accra")
                .shippingCountry("Ghana")
                .shippingPostalCode("00233")
                .build();

        when(customerStore.getCustomer(customerId)).thenReturn(Optional.of(customer));
        when(productStore.getProduct(productId1)).thenReturn(Optional.of(product1));
        when(productStore.getProduct(productId2)).thenReturn(Optional.of(product2));
        when(orderStore.createOrder(any(Orders.class), anyList())).thenReturn(savedOrder);

        OrderResponseDto response = orderService.placeOrder(request, customerId);

        Assertions.assertEquals(2, response.getItems().size());
        verify(orderStore).createOrder(any(Orders.class), anyList());
    }
}

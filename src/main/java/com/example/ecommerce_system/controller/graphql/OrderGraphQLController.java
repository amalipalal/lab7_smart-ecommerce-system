package com.example.ecommerce_system.controller.graphql;

import com.example.ecommerce_system.dto.orders.CreateOrderRequest;
import com.example.ecommerce_system.dto.orders.OrderRequestDto;
import com.example.ecommerce_system.dto.orders.OrderResponseDto;
import com.example.ecommerce_system.service.OrderService;
import com.example.ecommerce_system.util.SecurityContextHelper;
import lombok.AllArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;

@Controller
@AllArgsConstructor
public class OrderGraphQLController {
    private final OrderService orderService;

    @QueryMapping
    public List<OrderResponseDto> getCustomerOrders(
            @Argument(name = "limit") Integer limit,
            @Argument(name = "offset") Integer offset) {

        UUID userId = SecurityContextHelper.getCurrentUserId();
        int limitValue = limit != null ? limit : 10;
        int offsetValue = offset != null ? offset : 0;

        return orderService.getCustomerOrders(userId, limitValue, offsetValue);
    }

    @MutationMapping
    public OrderResponseDto placeOrder(
            @Argument @Validated(CreateOrderRequest.class) OrderRequestDto input) {
        UUID userId = SecurityContextHelper.getCurrentUserId();
        return orderService.placeOrder(input, userId);
    }
}

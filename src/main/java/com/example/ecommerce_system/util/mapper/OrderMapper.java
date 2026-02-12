package com.example.ecommerce_system.util.mapper;

import com.example.ecommerce_system.dto.orders.OrderResponseDto;
import com.example.ecommerce_system.model.Orders;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {OrderItemMapper.class})
public interface OrderMapper {

    @Mapping(source = "orderItems", target = "items")
    @Mapping(source = "status.statusName", target = "status")
    OrderResponseDto toDto(Orders orders);

    List<OrderResponseDto> toDtoList(List<Orders> orders);
}

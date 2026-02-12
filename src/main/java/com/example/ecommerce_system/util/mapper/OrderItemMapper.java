package com.example.ecommerce_system.util.mapper;

import com.example.ecommerce_system.dto.orders.OrderItemDto;
import com.example.ecommerce_system.model.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {

    @Mapping(source = "priceAtPurchase", target = "price")
    @Mapping(source = "product.productId", target = "productId")
    OrderItemDto toDto(OrderItem orderItem);
}

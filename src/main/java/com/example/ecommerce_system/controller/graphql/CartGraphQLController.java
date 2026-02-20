package com.example.ecommerce_system.controller.graphql;

import com.example.ecommerce_system.dto.cart.AddCartItem;
import com.example.ecommerce_system.dto.cart.CartItemRequestDto;
import com.example.ecommerce_system.dto.cart.CartItemResponseDto;
import com.example.ecommerce_system.dto.cart.UpdateCartItem;
import com.example.ecommerce_system.service.CartService;
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
public class CartGraphQLController {
    private final CartService cartService;

    @QueryMapping
    public List<CartItemResponseDto> getCustomerCartItems() {
        UUID userId = SecurityContextHelper.getCurrentUserId();
        return cartService.getCartItemsByCustomer(userId);
    }

    @MutationMapping
    public CartItemResponseDto addCartItem(
            @Argument @Validated(AddCartItem.class) CartItemRequestDto request) {
        UUID userId = SecurityContextHelper.getCurrentUserId();
        return cartService.addToCart(userId, request);
    }

    @MutationMapping
    public CartItemResponseDto updateCartItem(
            @Argument String cartItemId,
            @Argument @Validated(UpdateCartItem.class) CartItemRequestDto request) {
        UUID userId = SecurityContextHelper.getCurrentUserId();
        UUID cartItemUuid = UUID.fromString(cartItemId);
        return cartService.updateCartItem(userId, cartItemUuid, request);
    }

    @MutationMapping
    public Boolean removeFromCart(@Argument String cartItemId) {
        UUID userId = SecurityContextHelper.getCurrentUserId();
        UUID cartItemUuid = UUID.fromString(cartItemId);
        cartService.removeFromCart(userId, cartItemUuid);
        return true;
    }
}

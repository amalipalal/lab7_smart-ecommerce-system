package com.example.ecommerce_system.service;

import com.example.ecommerce_system.dto.cart.CartItemRequestDto;
import com.example.ecommerce_system.dto.cart.CartItemResponseDto;
import com.example.ecommerce_system.exception.cart.CartItemNotFoundException;
import com.example.ecommerce_system.exception.cart.CartItemAuthorizationException;
import com.example.ecommerce_system.exception.customer.CustomerNotFoundException;
import com.example.ecommerce_system.exception.product.ProductNotFoundException;
import com.example.ecommerce_system.model.Cart;
import com.example.ecommerce_system.model.CartItem;
import com.example.ecommerce_system.model.Customer;
import com.example.ecommerce_system.model.Product;
import com.example.ecommerce_system.repository.CartItemRepository;
import com.example.ecommerce_system.repository.CartRepository;
import com.example.ecommerce_system.repository.CustomerRepository;
import com.example.ecommerce_system.repository.ProductRepository;
import com.example.ecommerce_system.util.mapper.CartItemMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    private final CartItemMapper cartItemMapper;

    /**
     * Add a product to a customer's cart.
     * Creates a cart if the customer doesn't have one yet. Validates customer and product existence.
     */
    public CartItemResponseDto addToCart(UUID userId, CartItemRequestDto request) {
        var customer = retrieveCustomerFromRepository(userId);

        var product = retrieveProductFromRepository(request.getProductId());

        var cart = getOrCreateCartForCustomer(customer.getCustomerId());

        CartItem cartItem = CartItem.builder()
                .cartItemId(UUID.randomUUID())
                .cart(cart)
                .product(product)
                .quantity(request.getQuantity())
                .addedAt(Instant.now())
                .build();

        cart.getCartItems().add(cartItem);
        cartRepository.save(cart);
        return cartItemMapper.toDTO(cartItem);
    }

    private Customer retrieveCustomerFromRepository(UUID userId) {
        return customerRepository.findCustomerByUser_UserId(userId).orElseThrow(
                () -> new CustomerNotFoundException(userId.toString())
        );
    }

    private Product retrieveProductFromRepository(UUID productId) {
        return productRepository.findById(productId).orElseThrow(
                () -> new ProductNotFoundException(productId.toString())
        );
    }

    private Cart getOrCreateCartForCustomer(UUID customerId) {
        Optional<Cart> existingCart = cartRepository.findCartByCustomer_CustomerId(customerId);
        if (existingCart.isPresent()) return existingCart.get();

        var customer = retrieveCustomerFromRepository(customerId);

        Cart newCart = Cart.builder()
                .cartId(UUID.randomUUID())
                .customer(customer)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return cartRepository.save(newCart);
    }

    /**
     * Remove a cart item from the customer's cart.
     * Validates that the cart item exists and belongs to the customer before removal.
     */
    public void removeFromCart(UUID userId, UUID cartItemId) {
        var customer = retrieveCustomerFromRepository(userId);

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new CartItemNotFoundException(cartItemId.toString()));

        checkCartItemAuthorization(customer.getCustomerId(), cartItem);

        cartItemRepository.deleteById(cartItemId);
    }

    /**
     * Update the quantity of a cart item in the customer's cart.
     * Validates that the cart item exists and belongs to the customer. Returns the updated cart item with full product details.
     */
    @Transactional
    public CartItemResponseDto updateCartItem(UUID userId, UUID cartItemId, CartItemRequestDto request) {
        var customer = retrieveCustomerFromRepository(userId);

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new CartItemNotFoundException(cartItemId.toString()));

        checkCartItemAuthorization(customer.getCustomerId(), cartItem);

        cartItem.setQuantity(request.getQuantity());

        return cartItemMapper.toDTO(cartItem);
    }

    private void checkCartItemAuthorization(UUID customerId, CartItem cartItem) {
        Optional<Cart> cartOpt = cartRepository.findCartByCustomer_CustomerId(customerId);
        if (cartOpt.isEmpty() || !cartItem.getCart().getCartId().equals(cartOpt.get().getCartId())) {
            throw new CartItemAuthorizationException(cartItem.getCartItemId().toString());
        }
    }

    /**
     * Get all cart items for a customer.
     * Returns an empty list if the customer has no cart. Each cart item includes full product details.
     */
    public List<CartItemResponseDto> getCartItemsByCustomer(UUID userId) {
        var customer = retrieveCustomerFromRepository(userId);

        Optional<Cart> cartOpt = cartRepository.findCartByCustomer_CustomerId(customer.getCustomerId());

        if (cartOpt.isEmpty()) return List.of();

        List<CartItem> cartItems = cartOpt.get().getCartItems();

        return cartItemMapper.toDTOList(cartItems);
    }

}

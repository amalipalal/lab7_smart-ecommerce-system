package com.example.ecommerce_system.service;

import com.example.ecommerce_system.dto.review.ReviewRequestDto;
import com.example.ecommerce_system.dto.review.ReviewResponseDto;
import com.example.ecommerce_system.exception.customer.CustomerNotFoundException;
import com.example.ecommerce_system.exception.product.ProductNotFoundException;
import com.example.ecommerce_system.exception.review.CustomerHasNotOrderedProductException;
import com.example.ecommerce_system.model.Customer;
import com.example.ecommerce_system.model.Review;
import com.example.ecommerce_system.repository.CustomerRepository;
import com.example.ecommerce_system.repository.ProductRepository;
import com.example.ecommerce_system.store.OrdersStore;
import com.example.ecommerce_system.store.ReviewStore;
import com.example.ecommerce_system.util.mapper.CustomerMapper;
import com.example.ecommerce_system.util.mapper.ReviewMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class ReviewService {
    private final ReviewStore reviewStore;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final OrdersStore ordersStore;
    private final ReviewMapper reviewMapper;
    private final CustomerMapper customerMapper;

    /**
     * Create a new review for a product.
     * Validates that the product exists, the customer exists, and the customer has ordered and received (PROCESSED status) the product.
     */
    public ReviewResponseDto createReview(UUID productId, UUID customerId, ReviewRequestDto request) {
        var product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId.toString()));

        var customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId.toString()));

        validateCustomerHasProcessedProduct(customerId, productId);

        Review review = Review.builder()
                .reviewId(UUID.randomUUID())
                .product(product)
                .customerId(customerId)
                .rating(request.getRating())
                .comment(request.getComment())
                .createdAt(Instant.now())
                .build();

        Review savedReview = reviewStore.createReview(review);

        return mapToDto(savedReview, customer);
    }

    private void validateCustomerHasProcessedProduct(UUID customerId, UUID productId) {
        boolean hasProcessedOrder = ordersStore.hasProcessedOrderWithProduct(customerId, productId);

        if (!hasProcessedOrder) {
            throw new CustomerHasNotOrderedProductException(
                    customerId.toString(),
                    productId.toString()
            );
        }
    }

    private ReviewResponseDto mapToDto(Review review, Customer customer) {
        ReviewResponseDto dto = reviewMapper.toDTO(review);
        dto.setCustomer(customerMapper.toDTO(customer));
        return dto;
    }

    /**
     * Retrieve paginated reviews for a specific product.
     * Validates product existence before fetching reviews. Each review includes customer details.
     */
    public List<ReviewResponseDto> getReviewsByProduct(UUID productId, int limit, int offset) {
        productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId.toString()));

        List<Review> reviews = reviewStore.getReviewsByProduct(productId, limit, offset);

        return reviews.stream()
                .map(review -> {
                    Customer customer = customerRepository.findById(review.getCustomerId())
                            .orElseThrow(() -> new CustomerNotFoundException(review.getCustomerId().toString()));
                    return mapToDto(review, customer);
                })
                .toList();
    }
}

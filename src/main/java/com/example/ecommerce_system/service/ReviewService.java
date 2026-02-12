package com.example.ecommerce_system.service;

import com.example.ecommerce_system.dto.review.ReviewRequestDto;
import com.example.ecommerce_system.dto.review.ReviewResponseDto;
import com.example.ecommerce_system.exception.customer.CustomerNotFoundException;
import com.example.ecommerce_system.exception.product.ProductNotFoundException;
import com.example.ecommerce_system.exception.review.CustomerHasNotOrderedProductException;
import com.example.ecommerce_system.model.Customer;
import com.example.ecommerce_system.model.Product;
import com.example.ecommerce_system.model.Review;
import com.example.ecommerce_system.repository.CustomerRepository;
import com.example.ecommerce_system.repository.OrderRepository;
import com.example.ecommerce_system.repository.ProductRepository;
import com.example.ecommerce_system.repository.ReviewRepository;
import com.example.ecommerce_system.util.mapper.ReviewMapper;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;

    private final ReviewMapper reviewMapper;

    /**
     * Create a new review for a product.
     * Validates that the product exists, the customer exists, and the customer has ordered and received (PROCESSED status) the product.
     */
    public ReviewResponseDto createReview(UUID productId, UUID userId, ReviewRequestDto request) {
        var product = checkThatProductExists(productId);
        var customer = checkThatCustomerExists(userId);

        validateCustomerHasProcessedProduct(customer.getCustomerId(), productId);

        Review review = Review.builder()
                .reviewId(UUID.randomUUID())
                .product(product)
                .customer(customer)
                .rating(request.getRating())
                .comment(request.getComment())
                .createdAt(Instant.now())
                .build();

        Review savedReview = reviewRepository.save(review);
        return reviewMapper.toDTO(savedReview);
    }

    private Customer checkThatCustomerExists(UUID userId) {
        return customerRepository.findCustomerByUser_UserId(userId)
                .orElseThrow(() -> new CustomerNotFoundException(userId.toString()));
    }

    private Product checkThatProductExists(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId.toString()));
    }

    private void validateCustomerHasProcessedProduct(UUID customerId, UUID productId) {
        boolean hasProcessedOrder = orderRepository.hasProcessedOrderWithProduct(customerId, productId);

        if (!hasProcessedOrder) {
            throw new CustomerHasNotOrderedProductException(
                    customerId.toString(),
                    productId.toString()
            );
        }
    }

    /**
     * Retrieve paginated reviews for a specific product.
     * Validates product existence before fetching reviews. Each review includes customer details.
     */
    public List<ReviewResponseDto> getReviewsByProduct(UUID productId, int limit, int offset) {
        checkThatProductExists(productId);
        PageRequest pageRequest = PageRequest.of(
                offset,
                limit,
                Sort.by("createdAt").descending()
        );
        List<Review> reviews =  reviewRepository.findAllByProduct_ProductId(productId, pageRequest).getContent();
        return reviewMapper.toDTOList(reviews);
    }

    /**
     * Retrieve paginated reviews made by a specific customer.
     * Validates customer existence before fetching reviews.
     */
    public List<ReviewResponseDto> getReviewsByCustomer(UUID customerId, int limit, int offset) {
        var customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId.toString()));

        PageRequest pageRequest = PageRequest.of(
                offset,
                limit,
                Sort.by("createdAt").descending()
        );
        List<Review> reviews = reviewRepository.findAllByCustomer_CustomerId(customer.getCustomerId(), pageRequest).getContent();
        return reviewMapper.toDTOList(reviews);
    }
}

package com.example.ecommerce_system;

import com.example.ecommerce_system.dto.review.ReviewRequestDto;
import com.example.ecommerce_system.dto.review.ReviewResponseDto;
import com.example.ecommerce_system.exception.customer.CustomerNotFoundException;
import com.example.ecommerce_system.exception.product.ProductNotFoundException;
import com.example.ecommerce_system.exception.review.CustomerHasNotOrderedProductException;
import com.example.ecommerce_system.model.Customer;
import com.example.ecommerce_system.model.Product;
import com.example.ecommerce_system.model.Review;
import com.example.ecommerce_system.model.User;
import com.example.ecommerce_system.repository.CustomerRepository;
import com.example.ecommerce_system.repository.OrderRepository;
import com.example.ecommerce_system.repository.ProductRepository;
import com.example.ecommerce_system.repository.ReviewRepository;
import com.example.ecommerce_system.service.ReviewService;
import com.example.ecommerce_system.util.mapper.ReviewMapper;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ReviewMapper reviewMapper;

    @InjectMocks
    private ReviewService reviewService;

    private Product testProduct;
    private Customer testCustomer;
    private User testUser;
    private Review testReview;
    private ReviewRequestDto testRequestDto;
    private ReviewResponseDto testResponseDto;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();

        testUser = User.builder()
                .userId(userId)
                .email("test@example.com")
                .build();

        testCustomer = Customer.builder()
                .customerId(customerId)
                .firstName("John")
                .lastName("Doe")
                .user(testUser)
                .build();

        testProduct = Product.builder()
                .productId(productId)
                .name("Test Product")
                .price(99.99)
                .stockQuantity(10)
                .build();

        testReview = Review.builder()
                .reviewId(reviewId)
                .product(testProduct)
                .customer(testCustomer)
                .rating(5)
                .comment("Great product!")
                .createdAt(Instant.now())
                .build();

        testRequestDto = ReviewRequestDto.builder()
                .rating(5)
                .comment("Great product!")
                .build();

        testResponseDto = ReviewResponseDto.builder()
                .reviewId(reviewId)
                .productId(productId)
                .rating(5)
                .comment("Great product!")
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should create review successfully")
    void shouldCreateReviewSuccessfully() {
        UUID productId = testProduct.getProductId();
        UUID userId = testUser.getUserId();

        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(customerRepository.findCustomerByUser_UserId(userId)).thenReturn(Optional.of(testCustomer));
        when(orderRepository.hasProcessedOrderWithProduct(testCustomer.getCustomerId(), productId))
                .thenReturn(true);
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
        when(reviewMapper.toDTO(testReview)).thenReturn(testResponseDto);

        ReviewResponseDto response = reviewService.createReview(productId, userId, testRequestDto);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(testResponseDto.getReviewId(), response.getReviewId());
        Assertions.assertEquals(testResponseDto.getRating(), response.getRating());
        Assertions.assertEquals(testResponseDto.getComment(), response.getComment());

        verify(productRepository).findById(productId);
        verify(customerRepository).findCustomerByUser_UserId(userId);
        verify(orderRepository).hasProcessedOrderWithProduct(testCustomer.getCustomerId(), productId);
        verify(reviewRepository).save(any(Review.class));
        verify(reviewMapper).toDTO(testReview);
    }

    @Test
    @DisplayName("Should throw error when creating review for non-existing product")
    void shouldThrowWhenCreatingReviewForNonExistingProduct() {
        UUID productId = UUID.randomUUID();
        UUID userId = testUser.getUserId();

        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        Assertions.assertThrows(
                ProductNotFoundException.class,
                () -> reviewService.createReview(productId, userId, testRequestDto)
        );

        verify(productRepository).findById(productId);
        verify(customerRepository, never()).findCustomerByUser_UserId(any());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw error when creating review for non-existing customer")
    void shouldThrowWhenCreatingReviewForNonExistingCustomer() {
        UUID productId = testProduct.getProductId();
        UUID userId = UUID.randomUUID();

        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(customerRepository.findCustomerByUser_UserId(userId)).thenReturn(Optional.empty());

        Assertions.assertThrows(
                CustomerNotFoundException.class,
                () -> reviewService.createReview(productId, userId, testRequestDto)
        );

        verify(productRepository).findById(productId);
        verify(customerRepository).findCustomerByUser_UserId(userId);
        verify(orderRepository, never()).hasProcessedOrderWithProduct(any(), any());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw error when customer has not ordered product")
    void shouldThrowWhenCustomerHasNotOrderedProduct() {
        UUID productId = testProduct.getProductId();
        UUID userId = testUser.getUserId();

        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(customerRepository.findCustomerByUser_UserId(userId)).thenReturn(Optional.of(testCustomer));
        when(orderRepository.hasProcessedOrderWithProduct(testCustomer.getCustomerId(), productId))
                .thenReturn(false);

        Assertions.assertThrows(
                CustomerHasNotOrderedProductException.class,
                () -> reviewService.createReview(productId, userId, testRequestDto)
        );

        verify(productRepository).findById(productId);
        verify(customerRepository).findCustomerByUser_UserId(userId);
        verify(orderRepository).hasProcessedOrderWithProduct(testCustomer.getCustomerId(), productId);
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get reviews by product successfully")
    void shouldGetReviewsByProductSuccessfully() {
        UUID productId = testProduct.getProductId();
        int limit = 10;
        int offset = 0;

        List<Review> reviews = List.of(testReview);
        Page<Review> reviewsPage = new PageImpl<>(reviews);
        List<ReviewResponseDto> responseDtos = List.of(testResponseDto);

        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(reviewRepository.findAllByProduct_ProductId(eq(productId), any(PageRequest.class)))
                .thenReturn(reviewsPage);
        when(reviewMapper.toDTOList(reviews)).thenReturn(responseDtos);

        List<ReviewResponseDto> response = reviewService.getReviewsByProduct(productId, limit, offset);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(1, response.size());
        Assertions.assertEquals(testResponseDto.getReviewId(), response.get(0).getReviewId());

        verify(productRepository).findById(productId);
        verify(reviewRepository).findAllByProduct_ProductId(eq(productId), any(PageRequest.class));
        verify(reviewMapper).toDTOList(reviews);
    }

    @Test
    @DisplayName("Should throw error when getting reviews for non-existing product")
    void shouldThrowWhenGettingReviewsForNonExistingProduct() {
        UUID productId = UUID.randomUUID();

        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        Assertions.assertThrows(
                ProductNotFoundException.class,
                () -> reviewService.getReviewsByProduct(productId, 10, 0)
        );

        verify(productRepository).findById(productId);
        verify(reviewRepository, never()).findAllByProduct_ProductId(any(), any());
    }

    @Test
    @DisplayName("Should get reviews by customer successfully")
    void shouldGetReviewsByCustomerSuccessfully() {
        UUID customerId = testCustomer.getCustomerId();
        int limit = 10;
        int offset = 0;

        List<Review> reviews = List.of(testReview);
        Page<Review> reviewsPage = new PageImpl<>(reviews);
        List<ReviewResponseDto> responseDtos = List.of(testResponseDto);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));
        when(reviewRepository.findAllByCustomer_CustomerId(eq(customerId), any(PageRequest.class)))
                .thenReturn(reviewsPage);
        when(reviewMapper.toDTOList(reviews)).thenReturn(responseDtos);

        List<ReviewResponseDto> response = reviewService.getReviewsByCustomer(customerId, limit, offset);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(1, response.size());
        Assertions.assertEquals(testResponseDto.getReviewId(), response.get(0).getReviewId());

        verify(customerRepository).findById(customerId);
        verify(reviewRepository).findAllByCustomer_CustomerId(eq(customerId), any(PageRequest.class));
        verify(reviewMapper).toDTOList(reviews);
    }

    @Test
    @DisplayName("Should throw error when getting reviews for non-existing customer")
    void shouldThrowWhenGettingReviewsForNonExistingCustomer() {
        UUID customerId = UUID.randomUUID();

        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        Assertions.assertThrows(
                CustomerNotFoundException.class,
                () -> reviewService.getReviewsByCustomer(customerId, 10, 0)
        );

        verify(customerRepository).findById(customerId);
        verify(reviewRepository, never()).findAllByCustomer_CustomerId(any(), any());
    }

    @Test
    @DisplayName("Should handle pagination in get reviews by product")
    void shouldHandlePaginationInGetReviewsByProduct() {
        UUID productId = testProduct.getProductId();
        int limit = 5;
        int offset = 10;

        Page<Review> emptyPage = new PageImpl<>(List.of());

        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(reviewRepository.findAllByProduct_ProductId(eq(productId), any(PageRequest.class)))
                .thenReturn(emptyPage);
        when(reviewMapper.toDTOList(anyList())).thenReturn(List.of());

        List<ReviewResponseDto> response = reviewService.getReviewsByProduct(productId, limit, offset);

        Assertions.assertEquals(0, response.size());
        verify(reviewRepository).findAllByProduct_ProductId(eq(productId), any(PageRequest.class));
    }

    @Test
    @DisplayName("Should handle pagination in get reviews by customer")
    void shouldHandlePaginationInGetReviewsByCustomer() {
        UUID customerId = testCustomer.getCustomerId();
        int limit = 5;
        int offset = 10;

        Page<Review> emptyPage = new PageImpl<>(List.of());

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));
        when(reviewRepository.findAllByCustomer_CustomerId(eq(customerId), any(PageRequest.class)))
                .thenReturn(emptyPage);
        when(reviewMapper.toDTOList(anyList())).thenReturn(List.of());

        List<ReviewResponseDto> response = reviewService.getReviewsByCustomer(customerId, limit, offset);

        Assertions.assertEquals(0, response.size());
        verify(reviewRepository).findAllByCustomer_CustomerId(eq(customerId), any(PageRequest.class));
    }

    @Test
    @DisplayName("Should create review with different rating values")
    void shouldCreateReviewWithDifferentRatingValues() {
        UUID productId = testProduct.getProductId();
        UUID userId = testUser.getUserId();

        ReviewRequestDto requestWith1Star = ReviewRequestDto.builder()
                .rating(1)
                .comment("Poor product")
                .build();

        Review reviewWith1Star = Review.builder()
                .reviewId(UUID.randomUUID())
                .product(testProduct)
                .customer(testCustomer)
                .rating(1)
                .comment("Poor product")
                .createdAt(Instant.now())
                .build();

        ReviewResponseDto responseWith1Star = ReviewResponseDto.builder()
                .reviewId(reviewWith1Star.getReviewId())
                .productId(productId)
                .rating(1)
                .comment("Poor product")
                .createdAt(Instant.now())
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(customerRepository.findCustomerByUser_UserId(userId)).thenReturn(Optional.of(testCustomer));
        when(orderRepository.hasProcessedOrderWithProduct(testCustomer.getCustomerId(), productId))
                .thenReturn(true);
        when(reviewRepository.save(any(Review.class))).thenReturn(reviewWith1Star);
        when(reviewMapper.toDTO(reviewWith1Star)).thenReturn(responseWith1Star);

        ReviewResponseDto response = reviewService.createReview(productId, userId, requestWith1Star);

        Assertions.assertEquals(1, response.getRating());
        Assertions.assertEquals("Poor product", response.getComment());
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    @DisplayName("Should get multiple reviews by product with proper sorting")
    void shouldGetMultipleReviewsByProductWithProperSorting() {
        UUID productId = testProduct.getProductId();

        Review olderReview = Review.builder()
                .reviewId(UUID.randomUUID())
                .product(testProduct)
                .customer(testCustomer)
                .rating(3)
                .comment("Older review")
                .createdAt(Instant.now().minusSeconds(3600))
                .build();

        Review newerReview = Review.builder()
                .reviewId(UUID.randomUUID())
                .product(testProduct)
                .customer(testCustomer)
                .rating(5)
                .comment("Newer review")
                .createdAt(Instant.now())
                .build();

        List<Review> reviews = List.of(newerReview, olderReview); // Should be sorted by createdAt descending
        Page<Review> reviewsPage = new PageImpl<>(reviews);

        ReviewResponseDto newerResponseDto = ReviewResponseDto.builder()
                .reviewId(newerReview.getReviewId())
                .rating(5)
                .comment("Newer review")
                .build();

        ReviewResponseDto olderResponseDto = ReviewResponseDto.builder()
                .reviewId(olderReview.getReviewId())
                .rating(3)
                .comment("Older review")
                .build();

        List<ReviewResponseDto> responseDtos = List.of(newerResponseDto, olderResponseDto);

        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(reviewRepository.findAllByProduct_ProductId(eq(productId), any(PageRequest.class)))
                .thenReturn(reviewsPage);
        when(reviewMapper.toDTOList(reviews)).thenReturn(responseDtos);

        List<ReviewResponseDto> response = reviewService.getReviewsByProduct(productId, 10, 0);

        Assertions.assertEquals(2, response.size());
        Assertions.assertEquals("Newer review", response.get(0).getComment());
        Assertions.assertEquals("Older review", response.get(1).getComment());
        verify(reviewRepository).findAllByProduct_ProductId(eq(productId), any(PageRequest.class));
    }

    @Test
    @DisplayName("Should verify review creation with correct timestamp")
    void shouldVerifyReviewCreationWithCorrectTimestamp() {
        UUID productId = testProduct.getProductId();
        UUID userId = testUser.getUserId();

        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(customerRepository.findCustomerByUser_UserId(userId)).thenReturn(Optional.of(testCustomer));
        when(orderRepository.hasProcessedOrderWithProduct(testCustomer.getCustomerId(), productId))
                .thenReturn(true);
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
        when(reviewMapper.toDTO(testReview)).thenReturn(testResponseDto);

        Instant beforeCreation = Instant.now();
        reviewService.createReview(productId, userId, testRequestDto);
        Instant afterCreation = Instant.now();

        verify(reviewRepository).save(argThat(review ->
            review.getCreatedAt() != null &&
            !review.getCreatedAt().isBefore(beforeCreation) &&
            !review.getCreatedAt().isAfter(afterCreation)
        ));
    }
}

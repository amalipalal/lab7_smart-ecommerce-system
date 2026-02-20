package com.example.ecommerce_system.controller.graphql;

import com.example.ecommerce_system.dto.review.ReviewRequestDto;
import com.example.ecommerce_system.dto.review.ReviewResponseDto;
import com.example.ecommerce_system.service.ReviewService;
import com.example.ecommerce_system.util.SecurityContextHelper;
import lombok.AllArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
@AllArgsConstructor
public class ReviewGraphQLController {
    private final ReviewService reviewService;

    /**
     * GraphQL query to retrieve paginated reviews for a specific product.
     */
    @QueryMapping
    public List<ReviewResponseDto> getProductReviews(
            @Argument String productId,
            @Argument Integer limit,
            @Argument Integer offset) {
        UUID productUuid = UUID.fromString(productId);
        int actualLimit = limit != null ? limit : 10;
        int actualOffset = offset != null ? offset : 0;

        return reviewService.getReviewsByProduct(productUuid, actualLimit, actualOffset);
    }

    /**
     * GraphQL mutation to create a new review for a product.
     * Validates that the customer has ordered and received (processed) the product.
     */
    @MutationMapping
    public ReviewResponseDto createReview(
            @Argument String productId,
            @Argument ReviewRequestDto request) {
        UUID productUuid = UUID.fromString(productId);
        UUID userId = SecurityContextHelper.getCurrentUserId();

        return reviewService.createReview(productUuid, userId, request);
    }
}

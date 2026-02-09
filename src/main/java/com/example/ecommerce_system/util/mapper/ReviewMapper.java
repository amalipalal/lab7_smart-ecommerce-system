package com.example.ecommerce_system.util.mapper;

import com.example.ecommerce_system.dto.review.ReviewResponseDto;
import com.example.ecommerce_system.model.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(source = "product.productId", target = "productId")
    @Mapping(target = "customer", ignore = true)
    ReviewResponseDto toDTO(Review review);

    List<ReviewResponseDto> toDTOList(List<Review> reviews);
}

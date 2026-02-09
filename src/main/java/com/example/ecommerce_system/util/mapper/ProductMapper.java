package com.example.ecommerce_system.util.mapper;

import com.example.ecommerce_system.dto.product.ProductResponseDto;
import com.example.ecommerce_system.dto.product.ProductWithReviewsDto;
import com.example.ecommerce_system.model.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for Product entity.
 * Maps Product entity to various Product DTOs.
 */
@Mapper(componentModel = "spring", uses = {CategoryMapper.class, ReviewMapper.class})
public interface ProductMapper {

    @Mapping(source = "category.categoryId", target = "categoryId")
    @Mapping(source = "stockQuantity", target = "stock")
    ProductResponseDto toDTO(Product product);

    List<ProductResponseDto> toDTOList(List<Product> products);

    @Mapping(source = "category", target = "category")
    @Mapping(source = "stockQuantity", target = "stock")
    @Mapping(target = "reviews", ignore = true)
    ProductWithReviewsDto toProductWithReviewsDTO(Product product);
}

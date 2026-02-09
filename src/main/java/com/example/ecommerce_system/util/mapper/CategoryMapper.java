package com.example.ecommerce_system.util.mapper;

import com.example.ecommerce_system.dto.category.CategoryResponseDto;
import com.example.ecommerce_system.model.Category;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryResponseDto toDTO(Category category);

    List<CategoryResponseDto> toDTOList(List<Category> categories);
}

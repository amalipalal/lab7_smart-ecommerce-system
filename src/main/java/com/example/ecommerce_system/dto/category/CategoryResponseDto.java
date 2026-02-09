package com.example.ecommerce_system.dto.category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
@Builder
@Data
public class CategoryResponseDto {
    private final UUID categoryId;
    private final String name;
    private final String description;
    private final Instant createdAt;
    private final Instant updatedAt;
}

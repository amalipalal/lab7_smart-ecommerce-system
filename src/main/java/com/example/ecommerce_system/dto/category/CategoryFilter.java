package com.example.ecommerce_system.dto.category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class CategoryFilter {
    private String name;
    private String description;

    public boolean hasName() {
        return this.name != null && !this.name.trim().isEmpty();
    }

    public boolean hasDescription() {
        return this.description != null && !this.description.trim().isEmpty();
    }

    public boolean isEmpty() {
        return !hasName() && !hasDescription();
    }
}

package com.example.ecommerce_system.util;

import com.example.ecommerce_system.dto.product.ProductFilter;
import com.example.ecommerce_system.model.Product;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class ProductSpecification {

    public static Specification<Product> nameContains(String name) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Product> hasCategory(UUID categoryId) {
        return (root, query, cb) ->
                cb.equal(root.get("category").get("categoryId"), categoryId);
    }

    public static Specification<Product> buildSpecification(ProductFilter filter) {
        Specification<Product> spec = (root, query, criteriaBuilder) -> null;

        if (filter.hasName()) {
            spec = spec.and(nameContains(filter.getName()));
        }

        if (filter.hasCategoryId()) {
            spec = spec.and(hasCategory(filter.getCategoryId()));
        }

        return spec;
    }
}

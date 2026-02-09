package com.example.ecommerce_system.repository;

import com.example.ecommerce_system.model.Category;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    Optional<Category> findCategoryByName(String name);

    List<Category> findCategoriesByNameContainingIgnoreCase(String name, Pageable pageable);
}

package com.example.ecommerce_system.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Table(name = "review")
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class Review {
    @Id
    @Column(name = "review_id")
    private UUID reviewId;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private UUID customerId;

    private Integer rating;

    private String comment;

    @Column(name = "createdAt")
    private Instant createdAt;
}

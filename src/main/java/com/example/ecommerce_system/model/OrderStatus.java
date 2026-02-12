package com.example.ecommerce_system.model;

import com.example.ecommerce_system.enums.OrderStatusType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "order_statuses")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class OrderStatus {
    @Id
    @Column(name = "status_id")
    private UUID statusId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_name", nullable = false)
    private OrderStatusType statusName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}

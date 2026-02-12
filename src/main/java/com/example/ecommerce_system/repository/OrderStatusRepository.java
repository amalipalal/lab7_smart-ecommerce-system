package com.example.ecommerce_system.repository;

import com.example.ecommerce_system.model.OrderStatusType;
import com.example.ecommerce_system.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderStatusRepository extends JpaRepository<OrderStatus, UUID> {

    Optional<OrderStatus> findOrderStatusByStatusName(OrderStatusType statusName);
}

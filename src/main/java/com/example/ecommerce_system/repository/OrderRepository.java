package com.example.ecommerce_system.repository;

import com.example.ecommerce_system.model.Orders;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Orders, UUID>, JpaSpecificationExecutor<Orders> {

    List<Orders> findAllByCustomer_CustomerId(UUID customerId, Pageable pageable);

    @Query("SELECT COUNT(o) > 0 FROM Orders o JOIN o.orderItems oi WHERE o.customer.customerId = :customerId AND oi.product.productId = :productId AND o.status.statusName = 'PROCESSED'")
    boolean hasProcessedOrderWithProduct(@Param("customerId") UUID customerId, @Param("productId") UUID productId);
}

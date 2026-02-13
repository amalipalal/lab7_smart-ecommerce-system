package com.example.ecommerce_system.dto.orders;

import com.example.ecommerce_system.model.OrderStatusType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class OrderFilter {
    private UUID customerId;
    private OrderStatusType status;
    private Instant minOrderDate;
    private Instant maxOrderDate;
    private Double minAmount;
    private Double maxAmount;
    private String shippingCountry;
    private String shippingCity;

    public boolean hasCustomerId() {
        return this.customerId != null;
    }

    public boolean hasStatus() {
        return this.status != null;
    }

    public boolean hasMinOrderDate() {
        return this.minOrderDate != null;
    }

    public boolean hasMaxOrderDate() {
        return this.maxOrderDate != null;
    }

    public boolean hasOrderDateRange() {
        return hasMinOrderDate() || hasMaxOrderDate();
    }

    public boolean hasMinAmount() {
        return this.minAmount != null && this.minAmount >= 0;
    }

    public boolean hasMaxAmount() {
        return this.maxAmount != null && this.maxAmount >= 0;
    }

    public boolean hasAmountRange() {
        return hasMinAmount() || hasMaxAmount();
    }

    public boolean hasShippingCountry() {
        return this.shippingCountry != null && !this.shippingCountry.trim().isEmpty();
    }

    public boolean hasShippingCity() {
        return this.shippingCity != null && !this.shippingCity.trim().isEmpty();
    }

    public boolean isEmpty() {
        return !hasCustomerId() && !hasStatus() && !hasOrderDateRange() &&
               !hasAmountRange() && !hasShippingCountry() && !hasShippingCity();
    }
}

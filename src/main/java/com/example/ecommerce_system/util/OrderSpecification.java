package com.example.ecommerce_system.util;

import com.example.ecommerce_system.dto.orders.OrderFilter;
import com.example.ecommerce_system.model.OrderStatusType;
import com.example.ecommerce_system.model.Orders;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class OrderSpecification {

    public static Specification<Orders> hasCustomer(UUID customerId) {
        return (root, query, cb) ->
                cb.equal(root.get("customer").get("customerId"), customerId);
    }

    public static Specification<Orders> hasStatus(OrderStatusType status) {
        return (root, query, cb) ->
                cb.equal(root.get("status").get("statusName"), status);
    }

    public static Specification<Orders> orderDateAfter(Instant minOrderDate) {
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("orderDate"), minOrderDate);
    }

    public static Specification<Orders> orderDateBefore(Instant maxOrderDate) {
        return (root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("orderDate"), maxOrderDate);
    }

    public static Specification<Orders> amountGreaterThanOrEqual(Double minAmount) {
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("totalAmount"), minAmount);
    }

    public static Specification<Orders> amountLessThanOrEqual(Double maxAmount) {
        return (root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("totalAmount"), maxAmount);
    }

    public static Specification<Orders> shippingCountryEquals(String shippingCountry) {
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get("shippingCountry")), shippingCountry.toLowerCase());
    }

    public static Specification<Orders> shippingCityContains(String shippingCity) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("shippingCity")), "%" + shippingCity.toLowerCase() + "%");
    }

    public static Specification<Orders> buildSpecification(OrderFilter filter) {
        Specification<Orders> spec = (root, query, criteriaBuilder) -> null;

        return spec
                .and(buildIfPresent(filter::hasCustomerId, () -> hasCustomer(filter.getCustomerId())))
                .and(buildIfPresent(filter::hasStatus, () -> hasStatus(filter.getStatus())))
                .and(buildIfPresent(filter::hasMinOrderDate, () -> orderDateAfter(filter.getMinOrderDate())))
                .and(buildIfPresent(filter::hasMaxOrderDate, () -> orderDateBefore(filter.getMaxOrderDate())))
                .and(buildIfPresent(filter::hasMinAmount, () -> amountGreaterThanOrEqual(filter.getMinAmount())))
                .and(buildIfPresent(filter::hasMaxAmount, () -> amountLessThanOrEqual(filter.getMaxAmount())))
                .and(buildIfPresent(filter::hasShippingCountry, () -> shippingCountryEquals(filter.getShippingCountry())))
                .and(buildIfPresent(filter::hasShippingCity, () -> shippingCityContains(filter.getShippingCity())));
    }

    private static Specification<Orders> buildIfPresent(
            BooleanSupplier condition,
            Supplier<Specification<Orders>> specSupplier) {
        return condition.getAsBoolean() ? specSupplier.get() : null;
    }
}

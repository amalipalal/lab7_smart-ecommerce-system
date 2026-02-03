package com.example.ecommerce_system.dao.interfaces;

import com.example.ecommerce_system.exception.DaoException;
import com.example.ecommerce_system.model.OrderItem;

import java.sql.Connection;
import java.util.List;
import java.util.UUID;

public interface OrderItemDao {

    /**
     * Retrieve all order items for a specific order.
     *
     * @param connection the {@link java.sql.Connection} to use
     * @param orderId the order identifier
     * @return list of order items
     * @throws DaoException on DAO errors
     */
    List<OrderItem> findByOrderId(Connection connection, UUID orderId) throws DaoException;

    /**
     * Persist a new {@link OrderItem}.
     *
     * @param connection the {@link java.sql.Connection} to use
     * @param orderItem order item to save
     * @throws DaoException on DAO errors
     */
    void save(Connection connection, OrderItem orderItem) throws DaoException;

    /**
     * Persist multiple {@link OrderItem}s in batch.
     *
     * @param connection the {@link java.sql.Connection} to use
     * @param orderItems list of order items to save
     * @throws DaoException on DAO errors
     */
    void saveBatch(Connection connection, List<OrderItem> orderItems) throws DaoException;
}

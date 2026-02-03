package com.example.ecommerce_system.dao.interfaces;

import com.example.ecommerce_system.exception.DaoException;
import com.example.ecommerce_system.model.Orders;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrdersDao {

    /**
     * Find an order by id.
     *
     * @param connection the {@link java.sql.Connection} to use
     * @param orderId order identifier
     * @return optional order when found
     * @throws DaoException on DAO errors
     */
    Optional<Orders> findById(Connection connection, UUID orderId) throws DaoException;

    /**
     * Retrieve all orders with paging.
     *
     * @param connection the {@link java.sql.Connection} to use
     * @param limit maximum results
     * @param offset zero-based offset
     * @return list of orders
     * @throws DaoException on DAO errors
     */
    List<Orders> getAllOrders(Connection connection, int limit, int offset) throws DaoException;

    /**
     * Retrieve all orders of a customer with paging.
     *
     * @param connection the {@link java.sql.Connection} to use
     * @param customerId the {@link com.example.ecommerce_system.model.Customer}'s id
     * @param limit maximum results
     * @param offset zero-based offset
     * @return list of orders
     * @throws DaoException on DAO errors
     */
    List<Orders> getCustomerOrders(Connection connection, UUID customerId, int limit, int offset);

    /**
     * Persist a new {@link Orders}.
     *
     * @param connection the {@link java.sql.Connection} to use
     * @param order order to save
     * @throws DaoException on DAO errors
     */
    void save(Connection connection, Orders order) throws DaoException;

    /**
     * Update an order's status.
     *
     * @param connection the {@link java.sql.Connection} to use
     * @param order order with updated status
     * @throws DaoException on DAO errors
     */
    void update(Connection connection, Orders order) throws DaoException;

    /**
     * Delete an order by id.
     *
     * @param connection the {@link java.sql.Connection} to use
     * @param orderId order identifier
     * @throws DaoException on DAO errors
     */
    void delete(Connection connection, UUID orderId) throws DaoException;
}

package com.example.ecommerce_system.store;

import com.example.ecommerce_system.dao.interfaces.OrderItemDao;
import com.example.ecommerce_system.exception.DaoException;
import com.example.ecommerce_system.exception.DatabaseConnectionException;
import com.example.ecommerce_system.exception.orderitem.OrderItemCreationException;
import com.example.ecommerce_system.exception.orderitem.OrderItemRetrievalException;
import com.example.ecommerce_system.model.OrderItem;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Repository
public class OrderItemStore {
    private final DataSource dataSource;
    private final OrderItemDao orderItemDao;

    /**
     * Persist a new {@link com.example.ecommerce_system.model.OrderItem} inside a transaction.
     *
     * Delegates to {@link com.example.ecommerce_system.dao.interfaces.OrderItemDao#save(java.sql.Connection, com.example.ecommerce_system.model.OrderItem)}.
     * On success this method evicts relevant entries in the "order_items" cache via Spring Cache.
     *
     * @param orderItem the order item to create
     * @return the persisted {@link com.example.ecommerce_system.model.OrderItem}
     * @throws com.example.ecommerce_system.exception.orderitem.OrderItemCreationException when DAO save fails
     * @throws com.example.ecommerce_system.exception.DatabaseConnectionException when a DB connection cannot be obtained
     */
    @CacheEvict(value = "order_items", allEntries = true)
    public OrderItem createOrderItem(OrderItem orderItem) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                this.orderItemDao.save(conn, orderItem);
                conn.commit();
                return orderItem;
            } catch (DaoException e) {
                conn.rollback();
                throw new OrderItemCreationException(orderItem.getOrderItemId().toString());
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException(e);
        }
    }

    /**
     * Persist multiple {@link com.example.ecommerce_system.model.OrderItem}s in batch inside a transaction.
     *
     * Delegates to {@link com.example.ecommerce_system.dao.interfaces.OrderItemDao#saveBatch(java.sql.Connection, java.util.List)}.
     * On success this method evicts relevant entries in the "order_items" cache via Spring Cache.
     *
     * @param orderItems list of order items to create
     * @throws com.example.ecommerce_system.exception.orderitem.OrderItemCreationException when DAO save fails
     * @throws com.example.ecommerce_system.exception.DatabaseConnectionException when a DB connection cannot be obtained
     */
    @CacheEvict(value = "order_items", allEntries = true)
    public void createOrderItemsBatch(List<OrderItem> orderItems) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                this.orderItemDao.saveBatch(conn, orderItems);
                conn.commit();
            } catch (DaoException e) {
                conn.rollback();
                throw new OrderItemCreationException("batch");
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException(e);
        }
    }

    /**
     * Retrieve all order items for a specific order.
     *
     * Uses {@link com.example.ecommerce_system.dao.interfaces.OrderItemDao#findByOrderId(java.sql.Connection, java.util.UUID)}.
     * The returned value is cached in the "order_items" cache using Spring's cache abstraction.
     *
     * @param orderId order identifier
     * @return list of {@link com.example.ecommerce_system.model.OrderItem}
     * @throws com.example.ecommerce_system.exception.orderitem.OrderItemRetrievalException when DAO retrieval fails
     * @throws com.example.ecommerce_system.exception.DatabaseConnectionException when a DB connection cannot be obtained
     */
    @Cacheable(value = "order_items", key = "'order:' + #orderId")
    public List<OrderItem> getOrderItemsByOrderId(UUID orderId) {
        try (Connection conn = dataSource.getConnection()) {
            return this.orderItemDao.findByOrderId(conn, orderId);
        } catch (DaoException e) {
            throw new OrderItemRetrievalException(orderId.toString());
        } catch (SQLException e) {
            throw new DatabaseConnectionException(e);
        }
    }
}

package com.example.ecommerce_system.dao.impl;

import com.example.ecommerce_system.dao.interfaces.OrdersDao;
import com.example.ecommerce_system.exception.DaoException;
import com.example.ecommerce_system.model.OrderStatus;
import com.example.ecommerce_system.model.Orders;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Component
public class OrdersJdbcDao implements OrdersDao {
    private static final String FIND_BY_ID = """
        SELECT o.order_id, o.customer_id, os.status_name, o.order_date, o.total_amount,
               o.shipping_country, o.shipping_city, o.shipping_postal_code
        FROM orders o
        JOIN order_statuses os ON os.status_id = o.status_id
        WHERE order_id = ?
        """;

    private static final String ALL_ORDERS = """
        SELECT o.order_id, o.customer_id, os.status_name, o.order_date, o.total_amount,
               o.shipping_country, o.shipping_city, o.shipping_postal_code
        FROM orders o
        JOIN order_statuses os ON os.status_id = o.status_id
        ORDER BY o.order_date DESC
        LIMIT ? OFFSET ?
        """;

    private static final String FIND_BY_CUSTOMER = """
        SELECT o.order_id, o.customer_id, os.status_name, o.order_date, o.total_amount,
               o.shipping_country, o.shipping_city, o.shipping_postal_code
        FROM orders o
        JOIN order_statuses os ON os.status_id = o.status_id
        WHERE o.customer_id = ?
        ORDER BY o.order_date DESC
        LIMIT ? OFFSET ?
        """;

    private static final String SAVE = """
        INSERT INTO orders (
            order_id, customer_id, status_id, order_date, total_amount,
            shipping_country, shipping_city, shipping_postal_code
        )
        VALUES (?, ?, ?, (SELECT status_id FROM order_statuses WHERE status_name = ?), ?, ?, ?, ?)
        """;

    private static final String UPDATE = """
        UPDATE orders
        SET status_id = (SELECT status_id FROM order_statuses WHERE status_name = ?)
        WHERE order_id = ?
        """;

    private static final String DELETE = """
        DELETE FROM orders WHERE order_id = ?
        """;

    @Override
    public Optional<Orders> findById(Connection conn, UUID orderId) throws DaoException {
        try (PreparedStatement ps = conn.prepareStatement(FIND_BY_ID)) {
            ps.setObject(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Failed to fetch order " + orderId, e);
        }
        return Optional.empty();
    }

    private Orders map(ResultSet rs) throws SQLException {
        return new Orders(
                rs.getObject("order_id", UUID.class),
                rs.getObject("customer_id", UUID.class),
                OrderStatus.valueOf(rs.getString("status_name").toUpperCase()),
                rs.getTimestamp("order_date").toInstant(),
                rs.getDouble("total_amount"),
                rs.getString("shipping_country"),
                rs.getString("shipping_city"),
                rs.getString("shipping_postal_code")
        );
    }

    @Override
    public List<Orders> getAllOrders(Connection connection, int limit, int offset) throws DaoException {
        List<Orders> orders = new ArrayList<>();
        try(PreparedStatement ps = connection.prepareStatement(ALL_ORDERS)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try(ResultSet resultSet = ps.executeQuery()) {
                while (resultSet.next()) {
                    orders.add(map(resultSet));
                }
            }
        } catch ( SQLException e) {
            throw new DaoException("Failed to search orders by name", e);
        }

        return orders;
    }

    @Override
    public List<Orders> getCustomerOrders(Connection connection, UUID customerId, int limit, int offset) {
        List<Orders> orders = new ArrayList<>();
        try(PreparedStatement ps = connection.prepareStatement(FIND_BY_CUSTOMER)) {
            ps.setObject(1, customerId);
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            try(ResultSet resultSet = ps.executeQuery()) {
                while (resultSet.next()) {
                    orders.add(map(resultSet));
                }
            }
        } catch ( SQLException e) {
            throw new DaoException("Failed to search orders by customer", e);
        }

        return orders;
    }

    @Override
    public void save(Connection conn, Orders order) throws DaoException {
        try (PreparedStatement ps = conn.prepareStatement(SAVE)) {
            ps.setObject(1, order.getOrderId());
            ps.setObject(2, order.getCustomerId());
            ps.setString(3, order.getStatus().name());
            ps.setTimestamp(4, Timestamp.from(order.getOrderDate()));
            ps.setDouble(5, order.getTotalAmount());
            ps.setString(6, order.getShippingCountry());
            ps.setString(7, order.getShippingCity());
            ps.setString(8, order.getShippingPostalCode());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Failed to save order " + order.getOrderId(), e);
        }
    }

    @Override
    public void update(Connection conn, Orders order) throws DaoException {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE)) {
            ps.setString(1, order.getStatus().name());
            ps.setObject(2, order.getOrderId());
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected == 0) {
                throw new DaoException("Failed to update order " + order.getOrderId() + " - order not found");
            }
        } catch (SQLException e) {
            throw new DaoException("Failed to update order " + order.getOrderId(), e);
        }
    }

    @Override
    public void delete(Connection conn, UUID orderId) throws DaoException {
        try (PreparedStatement ps = conn.prepareStatement(DELETE)) {
            ps.setObject(1, orderId);
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected == 0) {
                throw new DaoException("Failed to delete order " + orderId + " - order not found");
            }
        } catch (SQLException e) {
            throw new DaoException("Failed to delete order " + orderId, e);
        }
    }
}

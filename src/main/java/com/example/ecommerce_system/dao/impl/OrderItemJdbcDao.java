package com.example.ecommerce_system.dao.impl;

import com.example.ecommerce_system.dao.interfaces.OrderItemDao;
import com.example.ecommerce_system.exception.DaoException;
import com.example.ecommerce_system.model.OrderItem;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class OrderItemJdbcDao implements OrderItemDao {

    private static final String FIND_BY_ORDER_ID = """
        SELECT order_item_id, order_id, product_id, quantity, price_at_purchase
        FROM order_item
        WHERE order_id = ?
        ORDER BY order_item_id
        """;

    private static final String SAVE = """
        INSERT INTO order_item (
            order_item_id, order_id, product_id, quantity, price_at_purchase
        )
        VALUES (?, ?, ?, ?, ?)
        """;

    @Override
    public List<OrderItem> findByOrderId(Connection connection, UUID orderId) throws DaoException {
        List<OrderItem> orderItems = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(FIND_BY_ORDER_ID)) {
            ps.setObject(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orderItems.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Failed to fetch order items for order " + orderId, e);
        }
        return orderItems;
    }

    @Override
    public void save(Connection connection, OrderItem orderItem) throws DaoException {
        try (PreparedStatement ps = connection.prepareStatement(SAVE)) {
            ps.setObject(1, orderItem.getOrderItemId());
            ps.setObject(2, orderItem.getOrder().getOrderId());
            ps.setObject(3, orderItem.getProduct().getProductId());
            ps.setInt(4, orderItem.getQuantity());
            ps.setDouble(5, orderItem.getPriceAtPurchase());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Failed to save order item " + orderItem.getOrderItemId(), e);
        }
    }

    @Override
    public void saveBatch(Connection connection, List<OrderItem> orderItems) throws DaoException {
        try (PreparedStatement ps = connection.prepareStatement(SAVE)) {
            for (OrderItem orderItem : orderItems) {
                ps.setObject(1, orderItem.getOrderItemId());
                ps.setObject(2, orderItem.getOrder().getOrderId());
                ps.setObject(3, orderItem.getProduct().getProductId());
                ps.setInt(4, orderItem.getQuantity());
                ps.setDouble(5, orderItem.getPriceAtPurchase());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new DaoException("Failed to save order items in batch", e);
        }
    }

    private OrderItem map(ResultSet rs) throws SQLException {
        return OrderItem.builder()
                .orderItemId(rs.getObject("order_item_id", UUID.class))
                .order(null)
                .product(null)
                .quantity(rs.getInt("quantity"))
                .priceAtPurchase(rs.getDouble("price_at_purchase"))
                .build();
    }
}

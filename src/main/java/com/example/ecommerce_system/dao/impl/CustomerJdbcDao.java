package com.example.ecommerce_system.dao.impl;

import com.example.ecommerce_system.dao.interfaces.CustomerDao;
import com.example.ecommerce_system.exception.DaoException;
import com.example.ecommerce_system.model.Customer;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.*;

@Repository
@NoArgsConstructor
public class CustomerJdbcDao implements CustomerDao {

    private static final String FIND_ALL = """
            SELECT c.customer_id, c.first_name, c.last_name, u.email, c.phone, u.created_at, c.is_active
            FROM customer c
            JOIN users u ON c.user_id = u.user_id
            LIMIT ? OFFSET ?
            """;

    private static final String FIND_BY_ID = """
            SELECT c.customer_id, c.first_name, c.last_name, u.email, c.phone, u.created_at, c.is_active
            FROM customer c
            JOIN users u ON c.user_id = u.user_id
            WHERE c.customer_id = ?
            """;

    private static final String FIND_BY_USER_ID = """
            SELECT c.customer_id, c.first_name, c.last_name, u.email, c.phone, u.created_at, c.is_active
            FROM customer c
            JOIN users u ON c.user_id = u.user_id
            WHERE c.user_id = ?
            """;

    private static final String FIND_BY_MULTIPLE_IDS = """
            SELECT c.customer_id, c.first_name, c.last_name, u.email, c.phone, u.created_at, c.is_active
            FROM customer c
            JOIN users u ON c.user_id = u.user_id
            WHERE c.customer_id IN 
            """;

    private static final String FIND_BY_EMAIL = """
            SELECT c.customer_id, c.first_name, c.last_name, u.email, c.phone, u.created_at, c.is_active
            FROM customer c
            JOIN users u ON c.user_id = u.user_id
            WHERE u.email = ?
            """;

    private static final String SAVE = """
            INSERT INTO customer
            (customer_id, user_id, first_name, last_name, phone)
            VALUES (?, ?, ?, ?, ?)
            """;

    private static final String UPDATE = """
            UPDATE customer
            SET first_name = ?, last_name = ?, phone = ?, is_active = ?
            WHERE customer_id = ?
            """;

    private static final String SEARCH = """
            SELECT c.customer_id, c.first_name, c.last_name, u.email, c.phone, u.created_at, c.is_active
            FROM customer c
            JOIN users u ON c.user_id = u.user_id
            WHERE LOWER(c.first_name) LIKE LOWER(?) 
               OR LOWER(c.last_name) LIKE LOWER(?) 
               OR LOWER(u.email) LIKE LOWER(?)
            LIMIT ? OFFSET ?
            """;

    @Override
    public List<Customer> findAll(Connection conn, int limit, int offset) throws DaoException {
        try (PreparedStatement preparedStatement = conn.prepareStatement(FIND_ALL)) {
            preparedStatement.setInt(1, limit);
            preparedStatement.setInt(2, offset);

            return executeQueryForList(preparedStatement);
        } catch (SQLException e) {
            throw new DaoException("Failed to load all products", e);
        }
    }

    private List<Customer> executeQueryForList(PreparedStatement ps) throws SQLException {
        List<Customer> results = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                results.add(map(rs));
            }
        }
        return results;
    }

    private Customer map(ResultSet resultSet) throws SQLException {
        return new Customer(
                resultSet.getObject("customer_id", UUID.class),
                resultSet.getString("first_name"),
                resultSet.getString("last_name"),
                resultSet.getString("email"),
                resultSet.getString("phone"),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getBoolean("is_active")
        );
    }

    @Override
    public Optional<Customer> findById(Connection conn, UUID customerId) throws DaoException {
        try (PreparedStatement ps = conn.prepareStatement(FIND_BY_ID)) {
            ps.setObject(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            throw new DaoException("Failed to fetch customer " + customerId, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Customer> findByUserId(Connection conn, UUID userId) throws DaoException {
        try (PreparedStatement ps = conn.prepareStatement(FIND_BY_USER_ID)) {
            ps.setObject(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            throw new DaoException("Failed to fetch customer with userId: " + userId, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Customer> findByIds(Connection conn, Set<UUID> customerIds) {
        String placeholders = String.join(", ", Collections.nCopies(customerIds.size(), "? "));
        String sql = FIND_BY_MULTIPLE_IDS + "(" + placeholders + ")";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = 1;
            for (UUID id : customerIds)
                ps.setObject(index++, id);

            try (ResultSet rs = ps.executeQuery()) {
                List<Customer> customers = new ArrayList<>();
                while (rs.next())
                    customers.add(map(rs));

                return customers;
            }

        } catch (SQLException e) {
            throw new DaoException("Failed to find customers by IDs", e);
        }
    }

    @Override
    public Optional<Customer> findByEmail(Connection conn, String email) throws DaoException {
        try (PreparedStatement ps = conn.prepareStatement(FIND_BY_EMAIL)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            throw new DaoException("Failed to find customer with email: " + email, e);
        }
        return Optional.empty();
    }

    @Override
    public void save(Connection conn, UUID userId, Customer customer) throws DaoException {
        try (PreparedStatement ps = conn.prepareStatement(SAVE)) {
            ps.setObject(1, customer.getCustomerId());
            ps.setObject(2, userId);
            ps.setString(3, customer.getFirstName());
            ps.setString(4, customer.getLastName());
            ps.setString(5, customer.getPhone());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Failed to save customer " + customer.getCustomerId(), e);
        }
    }

    @Override
    public void update(Connection conn, Customer customer) throws DaoException {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE)) {
            ps.setString(1, customer.getFirstName());
            ps.setString(2, customer.getLastName());
            ps.setString(3, customer.getPhone());
            ps.setBoolean(4, customer.isActive());
            ps.setObject(5, customer.getCustomerId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Failed to save customer " + customer.getCustomerId(), e);
        }
    }

    @Override
    public List<Customer> search(Connection conn, String query, int limit, int offset) throws DaoException {
        try (PreparedStatement ps = conn.prepareStatement(SEARCH)) {
            String searchPattern = "%" + query + "%";
            ps.setString(1, searchPattern);
            ps.setString(2, searchPattern);
            ps.setString(3, searchPattern);
            ps.setInt(4, limit);
            ps.setInt(5, offset);

            return executeQueryForList(ps);
        } catch (SQLException e) {
            throw new DaoException("Failed to search customers with query: " + query, e);
        }
    }
}
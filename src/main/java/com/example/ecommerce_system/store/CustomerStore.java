package com.example.ecommerce_system.store;

import com.example.ecommerce_system.dao.interfaces.CustomerDao;
import com.example.ecommerce_system.exception.customer.*;
import com.example.ecommerce_system.model.Customer;
import com.example.ecommerce_system.exception.*;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@AllArgsConstructor
@Repository
public class CustomerStore {
    private final DataSource dataSource;
    private final CustomerDao customerDao;

    /**
     * Update an existing {@link com.example.ecommerce_system.model.Customer} inside a transaction.
     *
     * Delegates to {@link com.example.ecommerce_system.dao.interfaces.CustomerDao#update(java.sql.Connection, com.example.ecommerce_system.model.Customer)}.
     * On success this method evicts relevant entries in the "customers" cache via Spring Cache.
     *
     * @param customer customer with updated fields
     * @return the updated {@link com.example.ecommerce_system.model.Customer}
     * @throws com.example.ecommerce_system.exception.customer.CustomerUpdateException when DAO update fails
     * @throws com.example.ecommerce_system.exception.DatabaseConnectionException when a DB connection cannot be obtained
     */
    @CacheEvict(value = "customers", allEntries = true)
    public Customer updateCustomer(Customer customer) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                this.customerDao.update(conn, customer);
                conn.commit();
                return customer;
            } catch (DaoException e) {
                conn.rollback();
                throw new CustomerUpdateException(customer.getCustomerId().toString());
            }
        } catch (SQLException e) {
            throw new DatabaseConnectionException(e);
        }
    }

    /**
     * Retrieve a customer by customer id.
     *
     * Uses {@link com.example.ecommerce_system.dao.interfaces.CustomerDao#findById(java.sql.Connection, java.util.UUID)}.
     * The returned value is cached in the "customers" cache using Spring's cache abstraction.
     *
     * @param customerId customer identifier
     * @return an {@link Optional} containing the {@link com.example.ecommerce_system.model.Customer} when found
     * @throws com.example.ecommerce_system.exception.customer.CustomerRetrievalException when DAO retrieval fails
     * @throws com.example.ecommerce_system.exception.DatabaseConnectionException when a DB connection cannot be obtained
     */
    @Cacheable(value = "customers", key = "'customer:' + #customerId")
    public Optional<Customer> getCustomer(UUID customerId) {
        try (Connection conn = dataSource.getConnection()) {
            return this.customerDao.findById(conn, customerId);
        } catch (DaoException e) {
            throw new CustomerRetrievalException(customerId.toString());
        } catch (SQLException e) {
            throw new DatabaseConnectionException(e);
        }
    }

    /**
     * Retrieve a customer by user id.</p>
     * Uses {@link com.example.ecommerce_system.dao.interfaces.CustomerDao#findById(java.sql.Connection, java.util.UUID)}.
     * The returned value is cached in the "customers" cache using Spring's cache abstraction.
     */
    @Cacheable(value = "customers", key = "'user:' + #userId")
    public Optional<Customer> getCustomerByUserId(UUID userId) {
        try (Connection conn = dataSource.getConnection()) {
            return this.customerDao.findByUserId(conn, userId);
        } catch (DaoException e) {
            throw new CustomerRetrievalException(userId.toString());
        } catch (SQLException e) {
            throw new DatabaseConnectionException(e);
        }
    }

    /**
     * Retrieve all customers with pagination.
     *
     * Delegates to {@link com.example.ecommerce_system.dao.interfaces.CustomerDao#findAll(java.sql.Connection, int, int)}.
     * Results are cached in the "customers" cache using Spring Cache.
     *
     * @param limit maximum number of results
     * @param offset zero-based offset
     * @return list of {@link com.example.ecommerce_system.model.Customer}
     * @throws com.example.ecommerce_system.exception.customer.CustomerRetrievalException when DAO retrieval fails
     * @throws com.example.ecommerce_system.exception.DatabaseConnectionException when a DB connection cannot be obtained
     */
    @Cacheable(value = "customers", key = "'all:' + #limit + ':' + #offset")
    public List<Customer> getAllCustomers(int limit, int offset) {
        try (Connection conn = dataSource.getConnection()) {
            return this.customerDao.findAll(conn, limit, offset);
        } catch (DaoException e) {
            throw new CustomerRetrievalException("all");
        } catch (SQLException e) {
            throw new DatabaseConnectionException(e);
        }
    }

    /**
     * Search customers by query string matching first name, last name, or email.
     *
     * Delegates to {@link com.example.ecommerce_system.dao.interfaces.CustomerDao#search(java.sql.Connection, String, int, int)}.
     * Results are cached in the "customers" cache using Spring Cache.
     *
     * @param query search query string
     * @param limit maximum number of results
     * @param offset zero-based offset
     * @return list of matching {@link com.example.ecommerce_system.model.Customer}
     * @throws com.example.ecommerce_system.exception.customer.CustomerSearchException when DAO search fails
     * @throws com.example.ecommerce_system.exception.DatabaseConnectionException when a DB connection cannot be obtained
     */
    @Cacheable(value = "customers", key = "'search:' + #query + ':' + #limit + ':' + #offset")
    public List<Customer> searchCustomers(String query, int limit, int offset) {
        try (Connection conn = dataSource.getConnection()) {
            return this.customerDao.search(conn, query, limit, offset);
        } catch (DaoException e) {
            throw new CustomerSearchException(query);
        } catch (SQLException e) {
            throw new DatabaseConnectionException(e);
        }
    }
}

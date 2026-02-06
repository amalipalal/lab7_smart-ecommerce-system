package com.example.ecommerce_system.dao.interfaces;

import com.example.ecommerce_system.exception.DaoException;
import com.example.ecommerce_system.model.Customer;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface CustomerDao {

    /**
     * Find all customers based on the pagination specifications
     *
     * @param connection the {@link java.sql.Connection}
     * @param limit maximum number of results
     * @param offset zero-based offset
     * @return list of customers
     * @throws DaoException on Dao errors
     */
    List<Customer> findAll(Connection connection, int limit, int offset) throws DaoException;

    /**
     * Find a customer by id.
     *
     * @param connection the {@link java.sql.Connection} to use
     * @param customerId customer identifier
     * @return optional customer when found
     * @throws DaoException on DAO errors
     */
    Optional<Customer> findById(Connection connection, UUID customerId) throws DaoException;

    /**
     * Find a customer by searching for user id
     * @param connection the {@link java.sql.Connection} to use
     * @param userId user identifier
     * @return optional customer when found
     * @throws DaoException on Dao errors
     */
    Optional<Customer> findByUserId(Connection connection, UUID userId) throws DaoException;

    /**
     * Find multiple customers by a set of ids.
     *
     * @param connection the {@link java.sql.Connection} to use
     * @param customerIds set of customer identifiers
     * @return list of found customers
     * @throws DaoException on Dao errors
     */
    List<Customer> findByIds(Connection connection, Set<UUID> customerIds) throws DaoException;

    /**
     * Find a customer by email.
     *
     * @param connection the {@link java.sql.Connection} to use
     * @param email customer email
     * @return optional customer when found
     * @throws DaoException on DAO errors
     */
    Optional<Customer> findByEmail(Connection connection, String email) throws DaoException;

    /**
     * Persist a new {@link Customer}.
     *
     * @param connection the {@link java.sql.Connection} to use
     * @param userId user linked to the customer
     * @param customer customer to save
     * @throws DaoException on DAO errors
     */
    void save(Connection connection, UUID userId, Customer customer) throws DaoException;

    /**
     * Update an existing {@link Customer}.
     *
     * @param connection the {@link java.sql.Connection} to use
     * @param customer customer to update
     * @throws DaoException on DAO errors
     */
    void update(Connection connection, Customer customer) throws DaoException;

    /**
     * Search customers by query string matching first name, last name, or email.
     *
     * @param connection the {@link java.sql.Connection} to use
     * @param query search query string
     * @param limit maximum number of results
     * @param offset zero-based offset
     * @return list of matching customers
     * @throws DaoException on DAO errors
     */
    List<Customer> search(Connection connection, String query, int limit, int offset) throws DaoException;
}

package com.example.ecommerce_system.service;

import com.example.ecommerce_system.dto.customer.CustomerRequestDto;
import com.example.ecommerce_system.dto.customer.CustomerResponseDto;
import com.example.ecommerce_system.exception.customer.CustomerNotFoundException;
import com.example.ecommerce_system.model.Customer;
import com.example.ecommerce_system.repository.CustomerRepository;
import com.example.ecommerce_system.util.mapper.CustomerMapper;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class CustomerService {

    private final CustomerMapper customerMapper;
    private final CustomerRepository customerRepository;

    /**
     * Retrieves a customer by their unique identifier.
     * The result is cached to improve performance for subsequent requests.
     */
    @Cacheable(value = "customers", key = "#customerId")
    public CustomerResponseDto getCustomer(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId.toString()));
        return customerMapper.toDTO(customer);
    }

    /**
     * Retrieves all customers with pagination.
     * Results are cached based on limit and offset parameters.
     * Uses zero-based page indexing where offset represents the page number.
     */
    @Cacheable(value = "paginated", key = "'all_customers_' + #limit + '_' + #offset")
    public List<CustomerResponseDto> getAllCustomers(int limit, int offset) {
        List<Customer> customers = customerRepository
                .findAll(PageRequest.of(offset, limit))
                .getContent();
        return customerMapper.toDTOList(customers);
    }

    /**
     * Searches for customers by query string matching first name, last name, or email.
     * Results are cached based on the search query and pagination parameters.
     * The search is case-insensitive and supports partial matching.
     */
    @Cacheable(value = "paginated", key = "'search_customers_' + #query + '_' + #limit + '_' + #offset")
    public List<CustomerResponseDto> searchCustomers(String query, int limit, int offset) {
        List<Customer> customers = customerRepository
                .searchCustomersByName(query, PageRequest.of(offset, limit))
                .getContent();
        return customerMapper.toDTOList(customers);
    }

    /**
     * Updates a customer's phone number and active status.
     * Only updates fields that are provided in the request (non-null values).
     * This operation evicts all customer-related caches to maintain data consistency.
     * Changes are persisted automatically due to the transactional context.
     */
    @CacheEvict(value = {"customers", "paginated"}, allEntries = true)
    @Transactional
    public CustomerResponseDto updateCustomer(UUID customerId, CustomerRequestDto request) {
        Customer existing = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId.toString()));

        if(request.getPhone() != null)
            existing.setPhone(request.getPhone());
        if(request.getActive() != null)
            existing.setActive(request.getActive());

        return customerMapper.toDTO(existing);
    }
}

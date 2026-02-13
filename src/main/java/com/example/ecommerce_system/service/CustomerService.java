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
     * Retrieve a customer by id.
     * <p>
     * Uses {@link com.example.ecommerce_system.store.CustomerStore#getCustomer(java.util.UUID)} and
     * throws {@link com.example.ecommerce_system.exception.customer.CustomerNotFoundException}
     * when no customer is found.
     */
    @Cacheable(value = "customers", key = "#customerId")
    public CustomerResponseDto getCustomer(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId.toString()));
        return customerMapper.toDTO(customer);
    }

    /**
     * Retrieve all customers with pagination.
     * <p>
     * Delegates to {@link com.example.ecommerce_system.store.CustomerStore#getAllCustomers(int, int)}.
     */
    @Cacheable(value = "paginated", key = "'all_customers_' + #limit + '_' + #offset")
    public List<CustomerResponseDto> getAllCustomers(int limit, int offset) {
        List<Customer> customers = customerRepository
                .findAll(PageRequest.of(offset, limit))
                .getContent();
        return customerMapper.toDTOList(customers);
    }

    /**
     * Search for customers by query string matching first name, last name, or email.
     * <p>
     * Delegates to {@link com.example.ecommerce_system.store.CustomerStore#searchCustomers(String, int, int)}.
     */
    @Cacheable(value = "paginated", key = "'search_customers_' + #query + '_' + #limit + '_' + #offset")
    public List<CustomerResponseDto> searchCustomers(String query, int limit, int offset) {
        List<Customer> customers = customerRepository
                .searchCustomersByName(query, PageRequest.of(offset, limit))
                .getContent();
        return customerMapper.toDTOList(customers);
    }

    /**
     * Update customer's details.
     * <p>
     * Validates presence of the customer via {@link com.example.ecommerce_system.store.CustomerStore#getCustomer(java.util.UUID)} and
     * delegates persistence to {@link com.example.ecommerce_system.store.CustomerStore#updateCustomer(com.example.ecommerce_system.model.Customer)}.
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

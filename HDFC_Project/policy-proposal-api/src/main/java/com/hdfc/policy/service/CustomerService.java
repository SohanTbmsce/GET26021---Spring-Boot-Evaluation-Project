package com.hdfc.policy.service;

import com.hdfc.policy.exception.BusinessException;
import com.hdfc.policy.exception.ResourceNotFoundException;
import com.hdfc.policy.model.Customer;
import com.hdfc.policy.model.dto.CustomerRequest;
import com.hdfc.policy.model.dto.CustomerResponse;
import com.hdfc.policy.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public CustomerResponse createCustomer(CustomerRequest request) {
        validateCustomerAge(request.getAge());

        Customer customer = new Customer();
        customer.setName(request.getName().trim());
        customer.setAge(request.getAge());
        customer.setPan(normalizePan(request.getPan()));

        Customer saved = customerRepository.save(customer);
        return toResponse(saved);
    }

    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public CustomerResponse getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
        return toResponse(customer);
    }

    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        Customer existing = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

        validateCustomerAge(request.getAge());

        existing.setName(request.getName().trim());
        existing.setAge(request.getAge());
        existing.setPan(normalizePan(request.getPan()));

        Customer updated = customerRepository.save(existing);
        return toResponse(updated);
    }

    public Customer getCustomerEntity(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
    }

    public void validateCustomerAge(int age) {
        if (age < PolicyConstants.MIN_CUSTOMER_AGE || age > PolicyConstants.MAX_CUSTOMER_AGE) {
            throw new BusinessException(
                    "Customer age must be between " + PolicyConstants.MIN_CUSTOMER_AGE
                            + " and " + PolicyConstants.MAX_CUSTOMER_AGE
            );
        }
    }

    private String normalizePan(String pan) {
        return StringUtils.hasText(pan) ? pan.trim().toUpperCase() : null;
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getAge(),
                customer.getPan()
        );
    }
}

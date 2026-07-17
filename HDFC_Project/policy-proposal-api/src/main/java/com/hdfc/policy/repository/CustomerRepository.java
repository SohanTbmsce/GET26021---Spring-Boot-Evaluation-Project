package com.hdfc.policy.repository;

import com.hdfc.policy.model.Customer;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class CustomerRepository {

    private final ConcurrentHashMap<Long, Customer> customers = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public Customer save(Customer customer) {
        if (customer.getId() == null) {
            customer.setId(idGenerator.getAndIncrement());
        }
        customers.put(customer.getId(), customer);
        return customer;
    }

    public Optional<Customer> findById(Long id) {
        return Optional.ofNullable(customers.get(id));
    }

    public List<Customer> findAll() {
        return new ArrayList<>(customers.values());
    }

    public boolean existsById(Long id) {
        return customers.containsKey(id);
    }
}

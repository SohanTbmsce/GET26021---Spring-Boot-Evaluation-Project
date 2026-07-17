package com.hdfc.policy.service;

import com.hdfc.policy.exception.BusinessException;
import com.hdfc.policy.model.dto.CustomerRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class CustomerServiceTest {

    @Autowired
    private CustomerService customerService;

    @Test
    void shouldRejectCustomerBelowMinimumAge() {
        CustomerRequest request = new CustomerRequest();
        request.setName("Young User");
        request.setAge(17);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> customerService.createCustomer(request)
        );

        assertEquals("Customer age must be between 18 and 65", exception.getMessage());
    }

    @Test
    void shouldRejectCustomerAboveMaximumAge() {
        CustomerRequest request = new CustomerRequest();
        request.setName("Senior User");
        request.setAge(66);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> customerService.createCustomer(request)
        );

        assertEquals("Customer age must be between 18 and 65", exception.getMessage());
    }

    @Test
    void shouldAcceptCustomerWithinValidAgeRange() {
        CustomerRequest request = new CustomerRequest();
        request.setName("Valid User");
        request.setAge(45);
        request.setPan("ABCDE1234F");

        assertEquals("Valid User", customerService.createCustomer(request).getName());
    }
}

package com.hdfc.policy.service;

import com.hdfc.policy.exception.BusinessException;
import com.hdfc.policy.model.Customer;
import com.hdfc.policy.model.Proposal;
import com.hdfc.policy.model.ProposalStatus;
import com.hdfc.policy.model.dto.CustomerRequest;
import com.hdfc.policy.model.dto.ProposalRequest;
import com.hdfc.policy.model.dto.ProposalResponse;
import com.hdfc.policy.repository.AuditRepository;
import com.hdfc.policy.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ProposalServiceTest {

    @Autowired
    private ProposalService proposalService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AuditRepository auditRepository;

    @Test
    void shouldRejectProposalWhenPremiumBelowMinimum() {
        CustomerRequest customerRequest = new CustomerRequest();
        customerRequest.setName("John Doe");
        customerRequest.setAge(30);
        customerRequest.setPan("ABCDE1234F");

        Long customerId = customerService.createCustomer(customerRequest).getId();

        ProposalRequest proposalRequest = buildValidProposalRequest(customerId);
        proposalRequest.setPremium(new BigDecimal("4999"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> proposalService.createProposal(proposalRequest)
        );

        assertEquals("Minimum premium is 5000", exception.getMessage());
    }

    @Test
    void shouldRejectProposalWhenNomineeSameAsCustomer() {
        CustomerRequest customerRequest = new CustomerRequest();
        customerRequest.setName("Jane Doe");
        customerRequest.setAge(35);
        customerRequest.setPan("ABCDE1234F");

        Long customerId = customerService.createCustomer(customerRequest).getId();

        ProposalRequest proposalRequest = buildValidProposalRequest(customerId);
        proposalRequest.setNominee("Jane Doe");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> proposalService.createProposal(proposalRequest)
        );

        assertEquals("Nominee cannot be the same as the customer", exception.getMessage());
    }

    @Test
    void shouldRejectProposalWhenPanMissingForHighPremium() {
        CustomerRequest customerRequest = new CustomerRequest();
        customerRequest.setName("Alex Smith");
        customerRequest.setAge(40);

        Long customerId = customerService.createCustomer(customerRequest).getId();

        ProposalRequest proposalRequest = buildValidProposalRequest(customerId);
        proposalRequest.setPremium(new BigDecimal("60000"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> proposalService.createProposal(proposalRequest)
        );

        assertEquals("PAN is required when premium exceeds 50000", exception.getMessage());
    }

    @Test
    void shouldRejectInvalidPaymentFrequency() {
        CustomerRequest customerRequest = new CustomerRequest();
        customerRequest.setName("Sam Wilson");
        customerRequest.setAge(28);
        customerRequest.setPan("ABCDE1234F");

        Long customerId = customerService.createCustomer(customerRequest).getId();

        ProposalRequest proposalRequest = buildValidProposalRequest(customerId);
        proposalRequest.setPaymentFrequency("WEEKLY");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> proposalService.createProposal(proposalRequest)
        );

        assertTrue(exception.getMessage().contains("Invalid payment frequency"));
    }

    @Test
    void shouldSubmitProposalAndCreateAuditRecord() {
        CustomerRequest customerRequest = new CustomerRequest();
        customerRequest.setName("Ravi Kumar");
        customerRequest.setAge(32);
        customerRequest.setPan("ABCDE1234F");

        Long customerId = customerService.createCustomer(customerRequest).getId();

        ProposalRequest proposalRequest = buildValidProposalRequest(customerId);
        ProposalResponse created = proposalService.createProposal(proposalRequest);

        assertEquals(ProposalStatus.DRAFT, created.getStatus());
        assertEquals(null, created.getPolicyNumber());

        long auditCountBefore = auditRepository.findAll().stream()
                .filter(audit -> "PROPOSAL_SUBMITTED".equals(audit.getAction()))
                .count();

        ProposalResponse submitted = proposalService.submitProposal(created.getId());

        assertEquals(ProposalStatus.SUBMITTED, submitted.getStatus());
        assertNotNull(submitted.getPolicyNumber());
        assertTrue(submitted.getPolicyNumber().startsWith("POL"));

        long auditCountAfter = auditRepository.findAll().stream()
                .filter(audit -> "PROPOSAL_SUBMITTED".equals(audit.getAction()))
                .count();

        assertEquals(auditCountBefore + 1, auditCountAfter);
    }

    @Test
    void shouldRejectResubmissionOfAlreadySubmittedProposal() {
        CustomerRequest customerRequest = new CustomerRequest();
        customerRequest.setName("Priya Sharma");
        customerRequest.setAge(29);
        customerRequest.setPan("ABCDE1234F");

        Long customerId = customerService.createCustomer(customerRequest).getId();
        ProposalResponse created = proposalService.createProposal(buildValidProposalRequest(customerId));
        proposalService.submitProposal(created.getId());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> proposalService.submitProposal(created.getId())
        );

        assertEquals("Proposal is already submitted", exception.getMessage());
    }

    @Test
    void shouldValidateCustomerAgeDuringProposalValidation() {
        Customer customer = new Customer();
        customer.setName("Old Customer");
        customer.setAge(70);
        customer.setPan("ABCDE1234F");
        customerRepository.save(customer);

        Proposal proposal = new Proposal();
        proposal.setCustomerId(customer.getId());
        proposal.setSumAssured(new BigDecimal("500000"));
        proposal.setTerm(20);
        proposal.setPremium(new BigDecimal("10000"));
        proposal.setNominee("Family Member");
        proposal.setPaymentFrequency("YEARLY");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> proposalService.validateProposalBusinessRules(proposal, customer)
        );

        assertEquals("Customer age must be between 18 and 65", exception.getMessage());
    }

    private ProposalRequest buildValidProposalRequest(Long customerId) {
        ProposalRequest request = new ProposalRequest();
        request.setCustomerId(customerId);
        request.setSumAssured(new BigDecimal("1000000"));
        request.setTerm(20);
        request.setPremium(new BigDecimal("25000"));
        request.setNominee("Family Member");
        request.setPaymentFrequency("YEARLY");
        return request;
    }
}

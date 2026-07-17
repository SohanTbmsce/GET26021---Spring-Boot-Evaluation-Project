package com.hdfc.policy.service;

import com.hdfc.policy.exception.BusinessException;
import com.hdfc.policy.exception.ResourceNotFoundException;
import com.hdfc.policy.model.Customer;
import com.hdfc.policy.model.Proposal;
import com.hdfc.policy.model.ProposalStatus;
import com.hdfc.policy.model.dto.ProposalRequest;
import com.hdfc.policy.model.dto.ProposalResponse;
import com.hdfc.policy.repository.ProposalRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Service
public class ProposalService {

    private final ProposalRepository proposalRepository;
    private final CustomerService customerService;
    private final AuditService auditService;

    public ProposalService(ProposalRepository proposalRepository,
                           CustomerService customerService,
                           AuditService auditService) {
        this.proposalRepository = proposalRepository;
        this.customerService = customerService;
        this.auditService = auditService;
    }

    public ProposalResponse createProposal(ProposalRequest request) {
        Customer customer = customerService.getCustomerEntity(request.getCustomerId());

        Proposal proposal = new Proposal();
        proposal.setCustomerId(request.getCustomerId());
        proposal.setSumAssured(request.getSumAssured());
        proposal.setTerm(request.getTerm());
        proposal.setPremium(request.getPremium());
        proposal.setNominee(request.getNominee().trim());
        proposal.setPaymentFrequency(request.getPaymentFrequency().trim().toUpperCase());
        proposal.setStatus(ProposalStatus.DRAFT);
        proposal.setPolicyNumber(null);

        validateProposalBusinessRules(proposal, customer);

        Proposal saved = proposalRepository.save(proposal);
        return toResponse(saved);
    }

    public ProposalResponse getProposalById(Long id) {
        Proposal proposal = proposalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found with id: " + id));
        return toResponse(proposal);
    }

    public ProposalResponse submitProposal(Long id) {
        Proposal proposal = proposalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found with id: " + id));

        if (proposal.getStatus() == ProposalStatus.SUBMITTED) {
            throw new BusinessException("Proposal is already submitted");
        }

        Customer customer = customerService.getCustomerEntity(proposal.getCustomerId());
        validateProposalBusinessRules(proposal, customer);

        String policyNumber = generatePolicyNumber();
        proposal.setPolicyNumber(policyNumber);
        proposal.setStatus(ProposalStatus.SUBMITTED);

        Proposal saved = proposalRepository.save(proposal);

        auditService.createAudit(
                "PROPOSAL_SUBMITTED",
                "Proposal " + saved.getId() + " submitted with policy number " + policyNumber
        );

        return toResponse(saved);
    }

    public void validateProposalBusinessRules(Proposal proposal, Customer customer) {
        customerService.validateCustomerAge(customer.getAge());

        if (!PolicyConstants.ALLOWED_POLICY_TERMS.contains(proposal.getTerm())) {
            throw new BusinessException(
                    "Policy term must be one of: " + PolicyConstants.ALLOWED_POLICY_TERMS
            );
        }

        if (proposal.getSumAssured().compareTo(PolicyConstants.MIN_SUM_ASSURED) < 0
                || proposal.getSumAssured().compareTo(PolicyConstants.MAX_SUM_ASSURED) > 0) {
            throw new BusinessException(
                    "Sum assured must be between " + PolicyConstants.MIN_SUM_ASSURED
                            + " and " + PolicyConstants.MAX_SUM_ASSURED
            );
        }

        if (proposal.getPremium().compareTo(PolicyConstants.MIN_PREMIUM) < 0) {
            throw new BusinessException(
                    "Minimum premium is " + PolicyConstants.MIN_PREMIUM
            );
        }

        if (proposal.getPremium().compareTo(PolicyConstants.PAN_REQUIRED_PREMIUM_THRESHOLD) > 0
                && !StringUtils.hasText(customer.getPan())) {
            throw new BusinessException(
                    "PAN is required when premium exceeds " + PolicyConstants.PAN_REQUIRED_PREMIUM_THRESHOLD
            );
        }

        if (!StringUtils.hasText(proposal.getNominee())) {
            throw new BusinessException("Nominee is mandatory");
        }

        if (proposal.getNominee().equalsIgnoreCase(customer.getName())) {
            throw new BusinessException("Nominee cannot be the same as the customer");
        }

        if (!PolicyConstants.ALLOWED_PAYMENT_FREQUENCIES.contains(proposal.getPaymentFrequency())) {
            throw new BusinessException(
                    "Invalid payment frequency. Allowed values: "
                            + PolicyConstants.ALLOWED_PAYMENT_FREQUENCIES
            );
        }
    }

    private String generatePolicyNumber() {
        return "POL" + System.currentTimeMillis();
    }

    private ProposalResponse toResponse(Proposal proposal) {
        ProposalResponse response = new ProposalResponse();
        response.setId(proposal.getId());
        response.setCustomerId(proposal.getCustomerId());
        response.setSumAssured(proposal.getSumAssured());
        response.setTerm(proposal.getTerm());
        response.setPremium(proposal.getPremium());
        response.setNominee(proposal.getNominee());
        response.setPaymentFrequency(proposal.getPaymentFrequency());
        response.setStatus(proposal.getStatus());
        response.setPolicyNumber(proposal.getPolicyNumber());
        return response;
    }
}

package com.hdfc.policy.controller;

import com.hdfc.policy.model.dto.ProposalRequest;
import com.hdfc.policy.model.dto.ProposalResponse;
import com.hdfc.policy.service.ProposalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/proposals")
public class ProposalController {

    private final ProposalService proposalService;

    public ProposalController(ProposalService proposalService) {
        this.proposalService = proposalService;
    }

    @PostMapping
    public ResponseEntity<ProposalResponse> createProposal(@Valid @RequestBody ProposalRequest request) {
        ProposalResponse response = proposalService.createProposal(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProposalResponse> getProposalById(@PathVariable Long id) {
        return ResponseEntity.ok(proposalService.getProposalById(id));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<ProposalResponse> submitProposal(@PathVariable Long id) {
        ProposalResponse response = proposalService.submitProposal(id);
        return ResponseEntity.ok(response);
    }
}

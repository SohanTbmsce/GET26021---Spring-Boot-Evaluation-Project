package com.hdfc.policy.controller;

import com.hdfc.policy.model.dto.AuditResponse;
import com.hdfc.policy.service.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/audits")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<List<AuditResponse>> getAllAudits() {
        return ResponseEntity.ok(auditService.getAllAudits());
    }
}

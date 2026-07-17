package com.hdfc.policy.service;

import com.hdfc.policy.model.Audit;
import com.hdfc.policy.model.dto.AuditResponse;
import com.hdfc.policy.repository.AuditRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditService {

    private final AuditRepository auditRepository;

    public AuditService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    public void createAudit(String action, String message) {
        Audit audit = new Audit();
        audit.setAction(action);
        audit.setMessage(message);
        audit.setTimestamp(LocalDateTime.now());
        auditRepository.save(audit);
    }

    public List<AuditResponse> getAllAudits() {
        return auditRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private AuditResponse toResponse(Audit audit) {
        return new AuditResponse(
                audit.getId(),
                audit.getAction(),
                audit.getMessage(),
                audit.getTimestamp()
        );
    }
}

package com.hdfc.policy.repository;

import com.hdfc.policy.model.Audit;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class AuditRepository {

    private final ConcurrentHashMap<Long, Audit> audits = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public Audit save(Audit audit) {
        if (audit.getId() == null) {
            audit.setId(idGenerator.getAndIncrement());
        }
        audits.put(audit.getId(), audit);
        return audit;
    }

    public List<Audit> findAll() {
        return audits.values().stream()
                .sorted(Comparator.comparing(Audit::getTimestamp).reversed())
                .toList();
    }
}

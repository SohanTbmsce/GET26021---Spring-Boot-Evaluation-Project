package com.hdfc.policy.repository;

import com.hdfc.policy.model.Proposal;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class ProposalRepository {

    private final ConcurrentHashMap<Long, Proposal> proposals = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public Proposal save(Proposal proposal) {
        if (proposal.getId() == null) {
            proposal.setId(idGenerator.getAndIncrement());
        }
        proposals.put(proposal.getId(), proposal);
        return proposal;
    }

    public Optional<Proposal> findById(Long id) {
        return Optional.ofNullable(proposals.get(id));
    }
}

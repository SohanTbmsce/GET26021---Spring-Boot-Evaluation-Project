package com.hdfc.policy.service;

import com.hdfc.policy.exception.ResourceNotFoundException;
import com.hdfc.policy.model.ReferenceCategory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ReferenceMasterService {

    private static final Map<ReferenceCategory, List<String>> REFERENCE_DATA = Map.of(
            ReferenceCategory.POLICY_TERM, PolicyConstants.ALLOWED_POLICY_TERMS.stream()
                    .map(String::valueOf)
                    .toList(),
            ReferenceCategory.PAYMENT_FREQUENCY, List.copyOf(PolicyConstants.ALLOWED_PAYMENT_FREQUENCIES)
    );

    public List<String> getReferenceData(String category) {
        ReferenceCategory referenceCategory;
        try {
            referenceCategory = ReferenceCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResourceNotFoundException("Reference category not found: " + category);
        }
        return REFERENCE_DATA.get(referenceCategory);
    }
}

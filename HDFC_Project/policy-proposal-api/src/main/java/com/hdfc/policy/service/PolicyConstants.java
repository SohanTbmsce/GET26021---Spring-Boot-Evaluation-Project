package com.hdfc.policy.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public final class PolicyConstants {

    public static final int MIN_CUSTOMER_AGE = 18;
    public static final int MAX_CUSTOMER_AGE = 65;
    public static final List<Integer> ALLOWED_POLICY_TERMS = List.of(10, 15, 20, 25, 30);
    public static final BigDecimal MIN_SUM_ASSURED = new BigDecimal("100000");
    public static final BigDecimal MAX_SUM_ASSURED = new BigDecimal("50000000");
    public static final BigDecimal MIN_PREMIUM = new BigDecimal("5000");
    public static final BigDecimal PAN_REQUIRED_PREMIUM_THRESHOLD = new BigDecimal("50000");
    public static final Set<String> ALLOWED_PAYMENT_FREQUENCIES = Set.of(
            "MONTHLY", "QUARTERLY", "HALF_YEARLY", "YEARLY"
    );

    private PolicyConstants() {
    }
}

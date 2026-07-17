package com.hdfc.policy.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class ProposalRequest {

    @NotNull(message = "Customer ID is required")
    @Min(value = 1, message = "Customer ID must be a positive number")
    private Long customerId;

    @NotNull(message = "Sum assured is required")
    @DecimalMin(value = "0.01", message = "Sum assured must be greater than zero")
    private BigDecimal sumAssured;

    @Min(value = 1, message = "Policy term must be a positive number")
    private int term;

    @NotNull(message = "Premium is required")
    @DecimalMin(value = "0.01", message = "Premium must be greater than zero")
    private BigDecimal premium;

    @NotBlank(message = "Nominee is required")
    @Size(max = 100, message = "Nominee name must not exceed 100 characters")
    private String nominee;

    @NotBlank(message = "Payment frequency is required")
    private String paymentFrequency;

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getSumAssured() {
        return sumAssured;
    }

    public void setSumAssured(BigDecimal sumAssured) {
        this.sumAssured = sumAssured;
    }

    public int getTerm() {
        return term;
    }

    public void setTerm(int term) {
        this.term = term;
    }

    public BigDecimal getPremium() {
        return premium;
    }

    public void setPremium(BigDecimal premium) {
        this.premium = premium;
    }

    public String getNominee() {
        return nominee;
    }

    public void setNominee(String nominee) {
        this.nominee = nominee;
    }

    public String getPaymentFrequency() {
        return paymentFrequency;
    }

    public void setPaymentFrequency(String paymentFrequency) {
        this.paymentFrequency = paymentFrequency;
    }
}

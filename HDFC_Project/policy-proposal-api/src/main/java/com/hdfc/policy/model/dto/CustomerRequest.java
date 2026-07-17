package com.hdfc.policy.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CustomerRequest {

    @NotBlank(message = "Customer name is required")
    @Size(max = 100, message = "Customer name must not exceed 100 characters")
    private String name;

    @Min(value = 1, message = "Age must be a positive number")
    @Max(value = 150, message = "Age must be a valid number")
    private int age;

    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]$", message = "PAN must be in valid format (e.g., ABCDE1234F)")
    private String pan;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getPan() {
        return pan;
    }

    public void setPan(String pan) {
        this.pan = pan;
    }
}

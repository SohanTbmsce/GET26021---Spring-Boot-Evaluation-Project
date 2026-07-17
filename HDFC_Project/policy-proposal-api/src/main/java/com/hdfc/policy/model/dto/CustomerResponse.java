package com.hdfc.policy.model.dto;

public class CustomerResponse {

    private Long id;
    private String name;
    private int age;
    private String pan;

    public CustomerResponse() {
    }

    public CustomerResponse(Long id, String name, int age, String pan) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.pan = pan;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

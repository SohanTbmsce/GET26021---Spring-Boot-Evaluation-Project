package com.hdfc.policy.model;

import java.time.LocalDateTime;

public class Audit {

    private Long id;
    private String action;
    private String message;
    private LocalDateTime timestamp;

    public Audit() {
    }

    public Audit(Long id, String action, String message, LocalDateTime timestamp) {
        this.id = id;
        this.action = action;
        this.message = message;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}

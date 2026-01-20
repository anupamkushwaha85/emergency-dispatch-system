package com.hackathon.emergency108.auth.dto;

import com.hackathon.emergency108.entity.UserRole;

public class SendOtpRequest {
    private String phone;
    private UserRole role;

    // Constructors
    public SendOtpRequest() {}

    public SendOtpRequest(String phone, UserRole role) {
        this.phone = phone;
        this.role = role;
    }

    // Getters and Setters
    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }
}

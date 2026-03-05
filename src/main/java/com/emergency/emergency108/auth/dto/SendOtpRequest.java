package com.emergency.emergency108.auth.dto;

import com.emergency.emergency108.entity.UserRole;

/**
 * Represents the SendOtpRequest component in the system.
 *
 * @author anupam kushwaha
 */
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

    /**
     * Set phone operation.
     * @param phone the phone
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * Get role operation.
     * @return the UserRole
     */
    public UserRole getRole() {
        return role;
    }

    /**
     * Set role operation.
     * @param role the role
     */
    public void setRole(UserRole role) {
        this.role = role;
    }
}

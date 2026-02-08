package com.emergency.emergency108.auth.dto;

import com.emergency.emergency108.entity.User;
import com.emergency.emergency108.entity.UserRole;
import com.emergency.emergency108.entity.DriverVerificationStatus;

public class AuthResponse {
    private String token;
    private Long userId;
    private String phone;
    private String name;
    private UserRole role;
    private DriverVerificationStatus verificationStatus;
    private boolean profileComplete;

    // Constructors
    public AuthResponse() {}

    public AuthResponse(String token, User user) {
        this.token = token;
        this.userId = user.getId();
        this.phone = user.getPhone();
        this.name = user.getName();
        this.role = user.getRole();
        this.verificationStatus = user.getDriverVerificationStatus();
        this.profileComplete = user.isProfileComplete();
    }

    // Getters and Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public DriverVerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(DriverVerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public boolean isProfileComplete() {
        return profileComplete;
    }

    public void setProfileComplete(boolean profileComplete) {
        this.profileComplete = profileComplete;
    }
}

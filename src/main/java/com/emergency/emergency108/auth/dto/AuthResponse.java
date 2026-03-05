package com.emergency.emergency108.auth.dto;

import com.emergency.emergency108.entity.User;
import com.emergency.emergency108.entity.UserRole;
import com.emergency.emergency108.entity.DriverVerificationStatus;

/**
 * Represents the AuthResponse component in the system.
 *
 * @author anupam kushwaha
 */
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

    /**
     * Set token operation.
     * @param token the token
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * Get user id operation.
     * @return the Long
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * Set user id operation.
     * @param userId the userId
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * Get phone operation.
     * @return the String
     */
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
     * Get name operation.
     * @return the String
     */
    public String getName() {
        return name;
    }

    /**
     * Set name operation.
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
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

    /**
     * Get verification status operation.
     * @return the DriverVerificationStatus
     */
    public DriverVerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    /**
     * Set verification status operation.
     * @param verificationStatus the verificationStatus
     */
    public void setVerificationStatus(DriverVerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    /**
     * Is profile complete operation.
     * @return the boolean
     */
    public boolean isProfileComplete() {
        return profileComplete;
    }

    /**
     * Set profile complete operation.
     * @param profileComplete the profileComplete
     */
    public void setProfileComplete(boolean profileComplete) {
        this.profileComplete = profileComplete;
    }
}

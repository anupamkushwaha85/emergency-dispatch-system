package com.emergency.emergency108.auth.token;

import com.emergency.emergency108.entity.UserRole;

/**
 * Represents the AuthTokenPayload component in the system.
 *
 * @author anupam kushwaha
 */
public class AuthTokenPayload {

    private Long userId;
    private UserRole role;

    public AuthTokenPayload(Long userId, UserRole role) {
        this.userId = userId;
        this.role = role;
    }

    /**
     * Get user id operation.
     * @return the Long
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * Get role operation.
     * @return the UserRole
     */
    public UserRole getRole() {
        return role;
    }
}

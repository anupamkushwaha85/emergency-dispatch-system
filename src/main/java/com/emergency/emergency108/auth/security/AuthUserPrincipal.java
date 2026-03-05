package com.emergency.emergency108.auth.security;

import com.emergency.emergency108.entity.UserRole;

/**
 * Represents the AuthUserPrincipal component in the system.
 *
 * @author anupam kushwaha
 */
public class AuthUserPrincipal {

    private final Long userId;
    private final UserRole role;
    private final boolean blocked;
    private final boolean driverVerified;

    public AuthUserPrincipal(
            Long userId,
            UserRole role,
            boolean blocked,
            boolean driverVerified
    ) {
        this.userId = userId;
        this.role = role;
        this.blocked = blocked;
        this.driverVerified = driverVerified;
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

    /* ---------- Role helpers ---------- */

    public boolean isDriver() {
        return role == UserRole.DRIVER;
    }

    /**
     * Is public user operation.
     * @return the boolean
     */
    public boolean isPublicUser() {
        return role == UserRole.PUBLIC;
    }

    /**
     * Is admin operation.
     * @return the boolean
     */
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    /* ---------- Guard helpers ---------- */

    public boolean isBlocked() {
        return blocked;
    }

    /**
     * Is driver verified operation.
     * @return the boolean
     */
    public boolean isDriverVerified() {
        return driverVerified;
    }
}

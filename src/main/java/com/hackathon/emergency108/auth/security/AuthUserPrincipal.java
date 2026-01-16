package com.hackathon.emergency108.auth.security;

import com.hackathon.emergency108.entity.UserRole;

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

    public Long getUserId() {
        return userId;
    }

    public UserRole getRole() {
        return role;
    }

    /* ---------- Role helpers ---------- */

    public boolean isDriver() {
        return role == UserRole.DRIVER;
    }

    public boolean isPublicUser() {
        return role == UserRole.PUBLIC;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    /* ---------- Guard helpers ---------- */

    public boolean isBlocked() {
        return blocked;
    }

    public boolean isDriverVerified() {
        return driverVerified;
    }
}

package com.emergency.emergency108.auth.token;

import com.emergency.emergency108.entity.UserRole;

public class AuthTokenPayload {

    private Long userId;
    private UserRole role;

    public AuthTokenPayload(Long userId, UserRole role) {
        this.userId = userId;
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public UserRole getRole() {
        return role;
    }
}

package com.hackathon.emergency108.auth.security;

import com.hackathon.emergency108.entity.UserRole;

public final class AuthContext {

    private static final ThreadLocal<AuthUserPrincipal> CURRENT =
            new ThreadLocal<>();

    private AuthContext() {
        // utility class
    }

    public static void set(AuthUserPrincipal principal) {
        CURRENT.set(principal);
    }

    public static AuthUserPrincipal get() {
        return CURRENT.get();
    }

    public static boolean isAuthenticated() {
        return CURRENT.get() != null;
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static Long getUserId() {
        return CURRENT.get() != null ? CURRENT.get().getUserId() : null;
    }

    public static boolean hasRole(UserRole role) {
        return CURRENT.get() != null && CURRENT.get().getRole() == role;
    }

}

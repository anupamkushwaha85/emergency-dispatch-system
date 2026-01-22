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
        AuthUserPrincipal principal = CURRENT.get();
        if (principal == null) {
            throw new com.hackathon.emergency108.auth.exception.UnauthenticatedException();
        }
        return principal;
    }
    
    public static AuthUserPrincipal getOrNull() {
        return CURRENT.get();
    }

    public static boolean isAuthenticated() {
        return CURRENT.get() != null;
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static Long getUserId() {
        AuthUserPrincipal principal = get(); // This will throw if null
        return principal.getUserId();
    }

    public static boolean hasRole(UserRole role) {
        AuthUserPrincipal principal = CURRENT.get();
        return principal != null && principal.getRole() == role;
    }

}

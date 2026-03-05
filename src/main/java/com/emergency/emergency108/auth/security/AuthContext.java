package com.emergency.emergency108.auth.security;

import com.emergency.emergency108.entity.UserRole;

public final class AuthContext {

    private static final ThreadLocal<AuthUserPrincipal> CURRENT = new ThreadLocal<>();

    private AuthContext() {
        // utility class
    }

    /**
     * Set operation.
     * @param principal the principal
     */
    public static void set(AuthUserPrincipal principal) {
        CURRENT.set(principal);
    }

    /**
     * Get operation.
     * @return the AuthUserPrincipal
     */
    public static AuthUserPrincipal get() {
        AuthUserPrincipal principal = CURRENT.get();
        if (principal == null) {
            throw new com.emergency.emergency108.auth.exception.UnauthenticatedException();
        }
        return principal;
    }

    /**
     * Get or null operation.
     * @return the AuthUserPrincipal
     */
    public static AuthUserPrincipal getOrNull() {
        return CURRENT.get();
    }

    /**
     * Is authenticated operation.
     * @return the boolean
     */
    public static boolean isAuthenticated() {
        return CURRENT.get() != null;
    }

    /**
     * Clear operation.
     */
    public static void clear() {
        CURRENT.remove();
    }

    /**
     * Get user id operation.
     * @return the Long
     */
    public static Long getUserId() {
        AuthUserPrincipal principal = get(); // This will throw if null
        return principal.getUserId();
    }

    /**
     * Has role operation.
     * @param role the role
     * @return the boolean
     */
    public static boolean hasRole(UserRole role) {
        AuthUserPrincipal principal = CURRENT.get();
        return principal != null && principal.getRole() == role;
    }

}

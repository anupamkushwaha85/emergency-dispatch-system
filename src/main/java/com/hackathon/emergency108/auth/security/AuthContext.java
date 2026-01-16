package com.hackathon.emergency108.auth.security;

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
}

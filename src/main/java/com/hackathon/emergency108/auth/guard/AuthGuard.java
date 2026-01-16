package com.hackathon.emergency108.auth.guard;

import com.hackathon.emergency108.auth.security.AuthContext;
import com.hackathon.emergency108.auth.security.AuthUserPrincipal;
import com.hackathon.emergency108.auth.exception.DriverNotVerifiedException;
import com.hackathon.emergency108.auth.exception.UnauthenticatedException;
import com.hackathon.emergency108.auth.exception.UserBlockedException;
import com.hackathon.emergency108.entity.UserRole;
import org.springframework.stereotype.Component;

@Component
public class AuthGuard {

    /**
     * Ensure any authenticated user
     */
    public void requireAuthenticated() {
        if (!AuthContext.isAuthenticated()) {
            throw new UnauthenticatedException();
        }
    }

    /**
     * Ensure user has specific role
     */
    public void requireRole(UserRole role) {
        requireAuthenticated();

        AuthUserPrincipal principal = AuthContext.get();

        if (principal.getRole() != role) {
            throw new UnauthenticatedException();
        }
    }

    /**
     * Ensure verified ambulance driver
     */
    public void requireVerifiedDriver() {
        requireRole(UserRole.DRIVER);

        AuthUserPrincipal principal = AuthContext.get();

        if (!principal.isDriverVerified()) {
            throw new DriverNotVerifiedException();
        }
    }

    /**
     * Ensure user is not blocked
     */
    public void requireActiveUser() {
        requireAuthenticated();

        AuthUserPrincipal principal = AuthContext.get();

        if (principal.isBlocked()) {
            throw new UserBlockedException();
        }
    }
}

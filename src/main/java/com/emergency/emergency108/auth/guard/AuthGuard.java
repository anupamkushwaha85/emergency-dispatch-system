package com.emergency.emergency108.auth.guard;

import com.emergency.emergency108.auth.security.AuthContext;
import com.emergency.emergency108.auth.security.AuthUserPrincipal;
import com.emergency.emergency108.auth.exception.DriverNotVerifiedException;
import com.emergency.emergency108.auth.exception.UnauthenticatedException;
import com.emergency.emergency108.auth.exception.UserBlockedException;
import com.emergency.emergency108.entity.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AuthGuard {

    private static final Logger log = LoggerFactory.getLogger(AuthGuard.class);

    /**
     * Ensure any authenticated user
     */
    public void requireAuthenticated() {
        AuthUserPrincipal principal = AuthContext.get();
        
        if (principal == null) {
            log.warn("Authentication required but no principal found in context");
            throw new UnauthenticatedException();
        }
        
        log.debug("Auth check passed - userId: {}, role: {}", principal.getUserId(), principal.getRole());
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
            log.warn("Driver {} is not verified", principal.getUserId());
            throw new DriverNotVerifiedException();
        }
        
        log.debug("Verified driver check passed - driverId: {}", principal.getUserId());
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

package com.emergency.emergency108.auth.security.filter;

import com.emergency.emergency108.auth.exception.UnauthenticatedException;
import com.emergency.emergency108.auth.security.AuthContext;
import com.emergency.emergency108.auth.security.AuthUserPrincipal;
import com.emergency.emergency108.auth.token.AuthTokenPayload;
import com.emergency.emergency108.auth.token.TokenService;
import com.emergency.emergency108.entity.DriverVerificationStatus;
import com.emergency.emergency108.entity.User;
import com.emergency.emergency108.entity.UserRole;
import com.emergency.emergency108.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Populates AuthContext for each request.
 * Clears context after request completion.
 */
@Component
public class AuthContextFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    private final UserRepository userRepository;

    public AuthContextFilter(TokenService tokenService, UserRepository userRepository) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Skip processing for OPTIONS requests (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // SECURITY CRITICAL: Clear stale context BEFORE and AFTER request
        AuthContext.clear();

        try {
            String authHeader = request.getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {

                String token = authHeader.substring(7);

                AuthTokenPayload payload = tokenService.validateAndParse(token);

                User user = userRepository.findById(payload.getUserId())
                        .orElseThrow(UnauthenticatedException::new);

                boolean driverVerified = user.getRole() == UserRole.DRIVER &&
                        user.getDriverVerificationStatus() == DriverVerificationStatus.VERIFIED;

                AuthUserPrincipal principal = new AuthUserPrincipal(
                        user.getId(),
                        user.getRole(),
                        user.isBlocked(),
                        driverVerified);

                AuthContext.set(principal);
                request.setAttribute("_auth_verified", true);
            }

            filterChain.doFilter(request, response);

        } finally {
            AuthContext.clear();
        }
    }

}

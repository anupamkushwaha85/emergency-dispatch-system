package com.emergency.emergency108.auth.security.filter;

import com.emergency.emergency108.auth.security.AuthContext;
import com.emergency.emergency108.auth.security.AuthUserPrincipal;
import com.emergency.emergency108.auth.token.AuthTokenPayload;
import com.emergency.emergency108.auth.token.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// @Component - DISABLED: AuthContextFilter handles full authentication with DB lookup
// This filter was overwriting the correct principal with hardcoded false values
public class JwtAuthFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    public JwtAuthFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                AuthTokenPayload payload =
                        tokenService.validateAndParse(token);

                AuthUserPrincipal principal =
                        new AuthUserPrincipal(
                                payload.getUserId(),
                                payload.getRole(),
                                false, // blocked - will be loaded from DB if needed
                                false  // driverVerified - will be loaded from DB if needed
                        );

                AuthContext.set(principal);

            } catch (Exception ignored) {
                // invalid token → treated as unauthenticated
            }
        }

        filterChain.doFilter(request, response);
    }
}

package com.hackathon.emergency108.auth.security.filter;

import com.hackathon.emergency108.auth.security.AuthContext;
import com.hackathon.emergency108.auth.security.AuthUserPrincipal;
import com.hackathon.emergency108.entity.UserRole;
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

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // 🔹 TEMP auth source (safe default)
            // Later this will come from JWT / OTP token
            String userIdHeader = request.getHeader("X-USER-ID");
            String roleHeader = request.getHeader("X-USER-ROLE");

            if (userIdHeader != null && roleHeader != null) {
                Long userId = Long.parseLong(userIdHeader);
                UserRole role = UserRole.valueOf(roleHeader);

                AuthUserPrincipal principal =
                        new AuthUserPrincipal(
                                userId,
                                role,
                                false, // driverVerified (temporary default)
                                false  // blocked (temporary default)
                        );

                AuthContext.set(principal);
            }

            filterChain.doFilter(request, response);

        } finally {
            // 🧹 MUST clear context to avoid thread leakage
            AuthContext.clear();
        }
    }
}

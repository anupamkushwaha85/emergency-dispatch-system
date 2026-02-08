package com.emergency.emergency108.auth.controller;

import com.emergency.emergency108.auth.token.AuthTokenPayload;
import com.emergency.emergency108.auth.token.TokenService;
import com.emergency.emergency108.entity.DriverVerificationStatus;
import com.emergency.emergency108.entity.User;
import com.emergency.emergency108.entity.UserRole;

import com.emergency.emergency108.repository.UserRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/legacy")
public class PublicAuthController {

    private final UserRepository userRepository;
    private final TokenService tokenService;

    public PublicAuthController(
            UserRepository userRepository,
            TokenService tokenService
    ) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
    }

    /**
     * Simple login endpoint for testing - creates user if doesn't exist
     * Returns permanent JWT token
     * 
     * NOTE: DRIVER role users are created as PENDING by default.
     * They must complete verification process before working.
     * 
     * DEPRECATED: Use /api/auth/send-otp and /api/auth/verify-otp instead
     */
    @PostMapping("/login")
    public Map<String, Object> simpleLogin(
            @RequestParam String phone,
            @RequestParam(required = false) String role
    ) {
        // Default role to PUBLIC if not specified
        UserRole userRole = role != null ? UserRole.valueOf(role.toUpperCase()) : UserRole.PUBLIC;
        
        // Find or create user
        User user = userRepository.findByPhone(phone)
                .orElseGet(() -> {
                    User u = new User();
                    u.setPhone(phone);
                    u.setName("User " + phone);
                    u.setEmail(phone + "@emergency108.local");
                    u.setRole(userRole);
                    u.setActive(true);
                    u.setBlocked(false);
                    u.setCreatedAt(LocalDateTime.now());
                    
                    // IMPORTANT: Drivers start as PENDING (awaiting verification)
                    // They must complete document upload and admin verification
                    if (userRole == UserRole.DRIVER) {
                        u.setDriverVerificationStatus(DriverVerificationStatus.PENDING);
                    } else {
                        u.setDriverVerificationStatus(DriverVerificationStatus.NOT_REQUIRED);
                    }
                    
                    return userRepository.save(u);
                });

        // Generate permanent token
        String token = tokenService.generate(
                new AuthTokenPayload(user.getId(), user.getRole())
        );

        return Map.of(
            "token", token,
            "user", Map.of(
                "id", user.getId(),
                "phone", user.getPhone(),
                "name", user.getName(),
                "role", user.getRole().toString(),
                "verificationStatus", user.getDriverVerificationStatus().toString()
            )
        );
    }
}

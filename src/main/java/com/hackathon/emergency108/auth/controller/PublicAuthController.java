package com.hackathon.emergency108.auth.controller;

import com.hackathon.emergency108.auth.service.OtpService;
import com.hackathon.emergency108.auth.token.AuthTokenPayload;
import com.hackathon.emergency108.auth.token.TokenService;
import com.hackathon.emergency108.entity.DriverVerificationStatus;
import com.hackathon.emergency108.entity.User;
import com.hackathon.emergency108.entity.UserRole;
import com.hackathon.emergency108.entity.VerificationStatus;
import com.hackathon.emergency108.repository.UserRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class PublicAuthController {

    private final OtpService otpService;
    private final UserRepository userRepository;
    private final TokenService tokenService;

    public PublicAuthController(
            OtpService otpService,
            UserRepository userRepository,
            TokenService tokenService
    ) {
        this.otpService = otpService;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
    }

    @PostMapping("/otp/send")
    public void sendOtp(@RequestParam String phoneOrEmail) {
        otpService.sendOtp(phoneOrEmail);
    }

    @PostMapping("/otp/verify")
    public Map<String, String> verifyOtp(
            @RequestParam String phoneOrEmail,
            @RequestParam String code
    ) {
        otpService.verifyOtp(phoneOrEmail, code);

        User user = userRepository.findByPhone(phoneOrEmail)
                .orElseGet(() -> {
                    User u = new User();
                    u.setPhone(phoneOrEmail);
                    u.setEmail(phoneOrEmail + "@placeholder.local"); // REQUIRED
                    u.setRole(UserRole.PUBLIC);
                    u.setActive(true);
                    u.setBlocked(false);
                    u.setVerificationStatus(VerificationStatus.VERIFIED); // REQUIRED
                    return userRepository.save(u);
                });



        String token =
                tokenService.generate(
                        new AuthTokenPayload(user.getId(), user.getRole())
                );

        return Map.of("token", token);
    }
}

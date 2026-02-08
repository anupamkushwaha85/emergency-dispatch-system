package com.emergency.emergency108.auth.service;

import com.emergency.emergency108.entity.User;
import com.emergency.emergency108.entity.UserRole;
import com.emergency.emergency108.entity.DriverVerificationStatus;
import com.emergency.emergency108.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);
    private static final int OTP_LENGTH = 6;
    private static final int OTP_VALIDITY_MINUTES = 5;

    @Autowired
    private UserRepository userRepository;

    /**
     * Generate and send OTP to user's phone
     * If user doesn't exist, create new user with the given role
     */
    @Transactional
    public String sendOtp(String phone, UserRole role) {
        logger.info("📱 Generating OTP for phone: {} with role: {}", phone, role);

        // Find or create user
        User user = userRepository.findByPhone(phone)
                .orElseGet(() -> createNewUser(phone, role));

        // Generate 6-digit OTP
        String otp = generateOtp();
        user.setOtp(otp);
        user.setOtpGeneratedAt(LocalDateTime.now());

        userRepository.save(user);

        // In production, send OTP via SMS gateway (Twilio, AWS SNS, etc.)
        logger.info("✅ OTP generated successfully for {}: {}", phone, otp);
        logger.warn("🔔 [MOCK SMS] Sending OTP {} to phone {}", otp, phone);

        return otp; // Remove this in production! Only for testing
    }

    /**
     * Verify OTP and optionally check admin passkey
     */
    @Transactional
    public User verifyOtp(String phone, String otp, String adminPasskey) {
        logger.info("🔐 Verifying OTP for phone: {}", phone);

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("User not found with phone: " + phone));

        // Check if OTP exists
        if (user.getOtp() == null || user.getOtpGeneratedAt() == null) {
            throw new RuntimeException("No OTP found. Please request a new OTP.");
        }

        // Check OTP expiry (5 minutes)
        LocalDateTime expiryTime = user.getOtpGeneratedAt().plusMinutes(OTP_VALIDITY_MINUTES);
        if (LocalDateTime.now().isAfter(expiryTime)) {
            throw new RuntimeException("OTP expired. Please request a new OTP.");
        }

        // Check OTP match
        if (!user.getOtp().equals(otp)) {
            throw new RuntimeException("Invalid OTP. Please try again.");
        }

        // ADMIN-specific validation
        if (user.getRole() == UserRole.ADMIN) {
            if (adminPasskey == null || adminPasskey.isEmpty()) {
                throw new RuntimeException("Admin passkey is required for admin login");
            }
            if (user.getAdminPasskey() == null) {
                throw new RuntimeException("Admin passkey not configured. Contact system administrator.");
            }
            if (!user.getAdminPasskey().equals(adminPasskey)) {
                throw new RuntimeException("Invalid admin passkey");
            }
            logger.info("✅ Admin passkey verified for user: {}", user.getId());
        }

        // Clear OTP after successful verification
        user.setOtp(null);
        user.setOtpGeneratedAt(null);
        user.setActive(true);

        userRepository.save(user);

        logger.info("✅ OTP verified successfully for user: {}", user.getId());
        return user;
    }

    /**
     * Create new user with default settings
     */
    private User createNewUser(String phone, UserRole role) {
        logger.info("🆕 Creating new user with phone: {} and role: {}", phone, role);

        User user = new User();
        user.setPhone(phone);
        user.setName("User " + phone); // Default name
        user.setEmail(phone + "@emergency108.local"); // Default email
        user.setRole(role);
        user.setActive(false); // Will be activated after OTP verification
        user.setBlocked(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setProfileComplete(false);

        // Set driver verification status
        if (role == UserRole.DRIVER) {
            user.setDriverVerificationStatus(DriverVerificationStatus.PENDING);
        } else {
            user.setDriverVerificationStatus(DriverVerificationStatus.NOT_REQUIRED);
        }

        return user;
    }

    /**
     * Generate random 6-digit OTP
     */
    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); // 6-digit number
        return String.valueOf(otp);
    }

    /**
     * Check if OTP is still valid (for resend functionality)
     */
    public boolean isOtpValid(String phone) {
        return userRepository.findByPhone(phone)
                .map(user -> {
                    if (user.getOtpGeneratedAt() == null) return false;
                    LocalDateTime expiryTime = user.getOtpGeneratedAt().plusMinutes(OTP_VALIDITY_MINUTES);
                    return LocalDateTime.now().isBefore(expiryTime);
                })
                .orElse(false);
    }
}



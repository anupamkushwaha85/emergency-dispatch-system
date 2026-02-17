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
    private static final int OTP_VALIDITY_MINUTES = 5;

    private final UserRepository userRepository;

    public OtpService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

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
        String otp = getMagicOtp(user);
        if (otp == null) {
            otp = generateOtp();
        }

        user.setOtp(otp);
        user.setOtpGeneratedAt(LocalDateTime.now());

        userRepository.save(user);

        // In production, send OTP via SMS gateway (Twilio, AWS SNS, etc.)
        logger.info("✅ OTP generated successfully for {}: {}", phone, otp);
        if (otp.equals("123456") || otp.equals("654321") || otp.equals("221029")) {
            logger.warn("🪄 Using Magic OTP for {}", phone);
        } else {
            logger.warn("🔔 [MOCK SMS] Sending OTP {} to phone {}", otp, phone);
        }

        return otp; // Remove this in production! Only for testing
    }

    private String getMagicOtp(User user) {
        String adminOtp = System.getenv("MAGIC_OTP_ADMIN");
        String userOtp = System.getenv("MAGIC_OTP_USER");
        String driverOtp = System.getenv("MAGIC_OTP_DRIVER");

        // 1. Specific Admin User (High Priority)
        // If phone is 9090221043, check env var first, otherwise default to 123456
        if ("9090221043".equals(user.getPhone())) {
            if (adminOtp != null && !adminOtp.isBlank()) {
                return adminOtp;
            }
            return "123456"; // Default fallback for this specific admin
        }

        // 2. Generic Role Checks
        if (user.getRole() == UserRole.DRIVER && driverOtp != null && !driverOtp.isBlank()) {
            return driverOtp;
        }

        if (user.getRole() == UserRole.PUBLIC && userOtp != null && !userOtp.isBlank()) {
            return userOtp;
        }

        return null;
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
            // Bypass passkey check for Magic Admin to allow frontend login without passkey
            // field
            if ("9090221043".equals(user.getPhone())) {
                logger.info("🪄 Skipping admin passkey check for Magic Admin: {}", phone);
            } else {
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

}

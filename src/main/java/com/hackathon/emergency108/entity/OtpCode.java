package com.hackathon.emergency108.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * OTP (One-Time Password) codes for authentication.
 * Used for phone/email verification during login/signup.
 */
@Entity
@Table(
    name = "otp_codes",
    indexes = {
        @Index(name = "idx_identifier_used", columnList = "identifier, used"),
        @Index(name = "idx_phone_email", columnList = "phone_or_email")
    }
)
public class OtpCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The 6-digit OTP code
     */
    @Column(name = "code", length = 255, nullable = false)
    private String code;

    /**
     * Phone number or email where OTP was sent
     */
    @Column(name = "phone_or_email", length = 255, nullable = false)
    private String phoneOrEmail;

    /**
     * Unique identifier for this OTP (UUID)
     */
    @Column(name = "identifier", length = 255, nullable = false)
    private String identifier;

    /**
     * When this OTP expires
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Whether this OTP has been used/verified
     */
    @Column(name = "used", nullable = false)
    private Boolean used = false;

    // Constructors
    public OtpCode() {
    }

    public OtpCode(String code, String phoneOrEmail, String identifier, int validityMinutes) {
        this.code = code;
        this.phoneOrEmail = phoneOrEmail;
        this.identifier = identifier;
        this.expiresAt = LocalDateTime.now().plusMinutes(validityMinutes);
        this.used = false;
    }

    // Business methods
    
    /**
     * Check if OTP is expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Mark OTP as used
     */
    public void markAsUsed() {
        this.used = true;
    }

    /**
     * Validate OTP code
     */
    public boolean isValid(String inputCode) {
        return !used && !isExpired() && code.equals(inputCode);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getPhoneOrEmail() {
        return phoneOrEmail;
    }

    public void setPhoneOrEmail(String phoneOrEmail) {
        this.phoneOrEmail = phoneOrEmail;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Boolean getUsed() {
        return used;
    }

    public void setUsed(Boolean used) {
        this.used = used;
    }
}

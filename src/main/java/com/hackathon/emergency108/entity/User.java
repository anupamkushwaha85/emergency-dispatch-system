package com.hackathon.emergency108.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Column;


@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    private String phone;


    @Column(unique = true)
    private String email;

    private String language;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;



    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DriverVerificationStatus driverVerificationStatus;


    @Column(nullable = false)
    private boolean active;

    private boolean blocked = false;

    /**
     * Number of times user cancelled emergency after confirmation deadline (100s) or after driver assigned.
     * Used for monitoring suspect behavior and potential penalties.
     */
    @Column(name = "suspect_count", nullable = false)
    private Integer suspectCount = 0;

    /**
     * Timestamp of most recent suspect cancellation.
     * Used for tracking patterns and time-based penalties.
     */
    @Column(name = "last_suspect_at")
    private LocalDateTime lastSuspectAt;

    private LocalDateTime createdAt;

    // OTP Authentication fields
    @Column(length = 6)
    private String otp;

    @Column(name = "otp_generated_at")
    private LocalDateTime otpGeneratedAt;

    @Column(name = "admin_passkey", length = 8)
    private String adminPasskey;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "document_url", length = 500)
    private String documentUrl;

    @Column(name = "is_profile_complete", nullable = false)
    private boolean isProfileComplete = false;


    // getters & setters (write manually or use Lombok later)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public DriverVerificationStatus getDriverVerificationStatus() {
        return driverVerificationStatus;
    }

    public void setDriverVerificationStatus(DriverVerificationStatus verificationStatus) {
        this.driverVerificationStatus = verificationStatus;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public LocalDateTime getOtpGeneratedAt() {
        return otpGeneratedAt;
    }

    public void setOtpGeneratedAt(LocalDateTime otpGeneratedAt) {
        this.otpGeneratedAt = otpGeneratedAt;
    }

    public String getAdminPasskey() {
        return adminPasskey;
    }

    public void setAdminPasskey(String adminPasskey) {
        this.adminPasskey = adminPasskey;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDocumentUrl() {
        return documentUrl;
    }

    public void setDocumentUrl(String documentUrl) {
        this.documentUrl = documentUrl;
    }

    public boolean isProfileComplete() {
        return isProfileComplete;
    }

    public void setProfileComplete(boolean profileComplete) {
        isProfileComplete = profileComplete;
    }

    public Integer getSuspectCount() {
        return suspectCount;
    }

    public void setSuspectCount(Integer suspectCount) {
        this.suspectCount = suspectCount;
    }

    public LocalDateTime getLastSuspectAt() {
        return lastSuspectAt;
    }

    public void setLastSuspectAt(LocalDateTime lastSuspectAt) {
        this.lastSuspectAt = lastSuspectAt;
    }
}

package com.emergency.emergency108.auth.dto;

/**
 * Represents the VerifyOtpRequest component in the system.
 *
 * @author anupam kushwaha
 */
public class VerifyOtpRequest {
    private String phone;
    private String otp;
    private String adminPasskey; // Optional, required only for ADMIN role

    // Constructors
    public VerifyOtpRequest() {}

    public VerifyOtpRequest(String phone, String otp) {
        this.phone = phone;
        this.otp = otp;
    }

    public VerifyOtpRequest(String phone, String otp, String adminPasskey) {
        this.phone = phone;
        this.otp = otp;
        this.adminPasskey = adminPasskey;
    }

    // Getters and Setters
    public String getPhone() {
        return phone;
    }

    /**
     * Set phone operation.
     * @param phone the phone
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * Get otp operation.
     * @return the String
     */
    public String getOtp() {
        return otp;
    }

    /**
     * Set otp operation.
     * @param otp the otp
     */
    public void setOtp(String otp) {
        this.otp = otp;
    }

    /**
     * Get admin passkey operation.
     * @return the String
     */
    public String getAdminPasskey() {
        return adminPasskey;
    }

    /**
     * Set admin passkey operation.
     * @param adminPasskey the adminPasskey
     */
    public void setAdminPasskey(String adminPasskey) {
        this.adminPasskey = adminPasskey;
    }
}

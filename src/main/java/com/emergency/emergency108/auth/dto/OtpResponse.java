package com.emergency.emergency108.auth.dto;

/**
 * Represents the OtpResponse component in the system.
 *
 * @author anupam kushwaha
 */
public class OtpResponse {
    private String message;
    private String phone;
    private String otpSent; // Only for testing - remove in production

    // Constructors
    public OtpResponse() {}

    public OtpResponse(String message, String phone) {
        this.message = message;
        this.phone = phone;
    }

    public OtpResponse(String message, String phone, String otpSent) {
        this.message = message;
        this.phone = phone;
        this.otpSent = otpSent;
    }

    // Getters and Setters
    public String getMessage() {
        return message;
    }

    /**
     * Set message operation.
     * @param message the message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Get phone operation.
     * @return the String
     */
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
     * Get otp sent operation.
     * @return the String
     */
    public String getOtpSent() {
        return otpSent;
    }

    /**
     * Set otp sent operation.
     * @param otpSent the otpSent
     */
    public void setOtpSent(String otpSent) {
        this.otpSent = otpSent;
    }
}

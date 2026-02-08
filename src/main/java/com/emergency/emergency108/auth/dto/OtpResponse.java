package com.emergency.emergency108.auth.dto;

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

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getOtpSent() {
        return otpSent;
    }

    public void setOtpSent(String otpSent) {
        this.otpSent = otpSent;
    }
}

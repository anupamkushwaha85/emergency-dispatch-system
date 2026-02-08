package com.emergency.emergency108.auth.dto;

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

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public String getAdminPasskey() {
        return adminPasskey;
    }

    public void setAdminPasskey(String adminPasskey) {
        this.adminPasskey = adminPasskey;
    }
}

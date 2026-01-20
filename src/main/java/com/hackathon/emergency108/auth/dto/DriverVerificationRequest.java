package com.hackathon.emergency108.auth.dto;

public class DriverVerificationRequest {
    private String documentUrl;

    // Constructors
    public DriverVerificationRequest() {}

    public DriverVerificationRequest(String documentUrl) {
        this.documentUrl = documentUrl;
    }

    // Getters and Setters
    public String getDocumentUrl() {
        return documentUrl;
    }

    public void setDocumentUrl(String documentUrl) {
        this.documentUrl = documentUrl;
    }
}

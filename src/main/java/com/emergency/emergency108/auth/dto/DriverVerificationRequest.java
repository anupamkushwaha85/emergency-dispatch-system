package com.emergency.emergency108.auth.dto;

/**
 * Represents the DriverVerificationRequest component in the system.
 *
 * @author anupam kushwaha
 */
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

    /**
     * Set document url operation.
     * @param documentUrl the documentUrl
     */
    public void setDocumentUrl(String documentUrl) {
        this.documentUrl = documentUrl;
    }
}

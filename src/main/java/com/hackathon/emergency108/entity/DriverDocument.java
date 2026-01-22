package com.hackathon.emergency108.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Driver verification documents.
 * Stores uploaded document URLs and verification numbers.
 */
@Entity
@Table(name = "driver_documents")
public class DriverDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Driver ID (references users table where role=DRIVER)
     */
    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    /**
     * Driver user ID (references users table)
     * Same as driver_id, kept for compatibility
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Document URL (single document)
     * Matches ER diagram: document_url TEXT
     */
    @Column(name = "document_url", columnDefinition = "TEXT")
    private String documentUrl;

    /**
     * Driver's ID proof number (Aadhaar, PAN, etc.)
     */
    @Column(name = "id_proof_number", length = 255)
    private String idProofNumber;

    /**
     * Driver's license number
     */
    @Column(name = "license_number", length = 255)
    private String licenseNumber;

    /**
     * When documents were submitted
     */
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    // Constructors
    public DriverDocument() {
    }

    public DriverDocument(Long driverId, Long userId, String documentUrl, String idProofNumber, String licenseNumber) {
        this.driverId = driverId;
        this.userId = userId;
        this.documentUrl = documentUrl;
        this.idProofNumber = idProofNumber;
        this.licenseNumber = licenseNumber;
        this.submittedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getDocumentUrl() {
        return documentUrl;
    }

    public void setDocumentUrl(String documentUrl) {
        this.documentUrl = documentUrl;
    }

    public String getIdProofNumber() {
        return idProofNumber;
    }

    public void setIdProofNumber(String idProofNumber) {
        this.idProofNumber = idProofNumber;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}

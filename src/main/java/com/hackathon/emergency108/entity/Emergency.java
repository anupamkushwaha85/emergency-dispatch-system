package com.hackathon.emergency108.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "emergencies")
public class Emergency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type;       // ACCIDENT, HEART, INJURY, OTHER
    private String severity;   // CRITICAL, MEDIUM, LOW
    @Column(name = "lat", nullable = false)
    private Double latitude;

    @Column(name = "lng", nullable = false)
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmergencyStatus status;

    /**
     * User who created this emergency (patient/bystander).
     * Used for authorization (tracking, updates).
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * Emergency source type for hybrid payment model.
     * GOVERNMENT: Free service (default)
     * PRIVATE: Paid service
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private EmergencySourceType sourceType = EmergencySourceType.GOVERNMENT;

    /**
     * Payment status (only applicable for PRIVATE emergencies).
     * NOT_REQUIRED for GOVERNMENT emergencies.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.NOT_REQUIRED;

    /**
     * Calculated fare amount for PRIVATE emergencies.
     * NULL for GOVERNMENT emergencies.
     */
    @Column(name = "fare_amount")
    private Double fareAmount;

    /**
     * Payment gateway intent/transaction ID.
     * Used to track payment with Razorpay/Stripe.
     * NULL for GOVERNMENT emergencies or unpaid PRIVATE.
     */
    @Column(name = "payment_intent_id")
    private String paymentIntentId;

    @Version
    @Column(nullable = false)
    private Long version;



    @Column(columnDefinition = "TEXT")
    private String aiFirstAid;

    @Column(columnDefinition = "TEXT")
    private String aiDoctorSummary;

    private LocalDateTime createdAt;

    @Column(name = "status_updated_at")
    private LocalDateTime statusUpdatedAt;


    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.statusUpdatedAt = this.createdAt;
    }

    // getters & setters
    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public EmergencyStatus getStatus() {
        return status;
    }

    public void setStatus(EmergencyStatus status) {
        this.status = status;
        this.statusUpdatedAt = LocalDateTime.now();
    }

    public String getAiFirstAid() {
        return aiFirstAid;
    }

    public void setAiFirstAid(String aiFirstAid) {
        this.aiFirstAid = aiFirstAid;
    }

    public String getAiDoctorSummary() {
        return aiDoctorSummary;
    }

    public void setAiDoctorSummary(String aiDoctorSummary) {
        this.aiDoctorSummary = aiDoctorSummary;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getStatusUpdatedAt() {
        return statusUpdatedAt;
    }

    public void setStatusUpdatedAt(LocalDateTime statusUpdatedAt) {
        this.statusUpdatedAt = statusUpdatedAt;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public EmergencySourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(EmergencySourceType sourceType) {
        this.sourceType = sourceType;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public Double getFareAmount() {
        return fareAmount;
    }

    public void setFareAmount(Double fareAmount) {
        this.fareAmount = fareAmount;
    }

    public String getPaymentIntentId() {
        return paymentIntentId;
    }

    public void setPaymentIntentId(String paymentIntentId) {
        this.paymentIntentId = paymentIntentId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}

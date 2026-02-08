package com.emergency.emergency108.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "emergencies")
public class Emergency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type; // ACCIDENT, HEART, INJURY, OTHER
    private String severity; // CRITICAL, MEDIUM, LOW
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

    /**
     * Suspect cancellation flag.
     * True if user cancelled emergency after confirmation deadline (100s) or after
     * driver assigned.
     */
    @Column(name = "is_suspect_cancellation", nullable = false)
    private Boolean isSuspectCancellation = false;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(columnDefinition = "TEXT")
    private String aiFirstAid;

    @Column(columnDefinition = "TEXT")
    private String aiDoctorSummary;

    private LocalDateTime createdAt;

    /**
     * User must confirm within 100 seconds after creation.
     * If not confirmed, emergency is auto-dispatched.
     */
    @Column(name = "confirmation_deadline")
    private LocalDateTime confirmationDeadline;

    @Column(name = "status_updated_at")
    private LocalDateTime statusUpdatedAt;

    /**
     * Timestamp when emergency was marked COMPLETED.
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Target hospital latitude coordinate.
     */
    @Column(name = "hospital_latitude")
    private Double hospitalLatitude;

    /**
     * Target hospital longitude coordinate.
     */
    @Column(name = "hospital_longitude")
    private Double hospitalLongitude;

    /**
     * Distance in meters when completion was attempted.
     * Used to validate 100m proximity requirement.
     */
    @Column(name = "distance_to_hospital")
    private Double distanceToHospital;

    @Enumerated(EnumType.STRING)
    @Column(name = "emergency_for", nullable = false)
    private EmergencyFor emergencyFor = EmergencyFor.UNKNOWN;

    @Enumerated(EnumType.STRING)
    @Column(name = "contact_notification_status", nullable = false)
    private ContactNotificationStatus contactNotificationStatus = ContactNotificationStatus.PENDING;

    /**
     * Deadline for user to confirm ownership (SELF vs OTHER).
     * If passed, defaults to SELF.
     * Default: CreatedAt + 30 seconds.
     */
    @Column(name = "confirm_ownership_deadline")
    private LocalDateTime confirmOwnershipDeadline;

    /**
     * Structured AI assessment for Driver/Hospital (JSON).
     */
    @Column(columnDefinition = "TEXT")
    private String aiAssessment;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.statusUpdatedAt = this.createdAt;
        // Set confirmation deadline to 100 seconds after creation (Dispatch)
        this.confirmationDeadline = this.createdAt.plusSeconds(100);
        // Set ownership decision deadline to 100 seconds (Safety Net - Auto Dispatch)
        this.confirmOwnershipDeadline = this.createdAt.plusSeconds(100);
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

    public Boolean getIsSuspectCancellation() {
        return isSuspectCancellation;
    }

    public void setIsSuspectCancellation(Boolean isSuspectCancellation) {
        this.isSuspectCancellation = isSuspectCancellation;
    }

    public LocalDateTime getConfirmationDeadline() {
        return confirmationDeadline;
    }

    public void setConfirmationDeadline(LocalDateTime confirmationDeadline) {
        this.confirmationDeadline = confirmationDeadline;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Double getHospitalLatitude() {
        return hospitalLatitude;
    }

    public void setHospitalLatitude(Double hospitalLatitude) {
        this.hospitalLatitude = hospitalLatitude;
    }

    public Double getHospitalLongitude() {
        return hospitalLongitude;
    }

    public void setHospitalLongitude(Double hospitalLongitude) {
        this.hospitalLongitude = hospitalLongitude;
    }

    public Double getDistanceToHospital() {
        return distanceToHospital;
    }

    public void setDistanceToHospital(Double distanceToHospital) {
        this.distanceToHospital = distanceToHospital;
    }

    public EmergencyFor getEmergencyFor() {
        return emergencyFor;
    }

    public void setEmergencyFor(EmergencyFor emergencyFor) {
        this.emergencyFor = emergencyFor;
    }

    public ContactNotificationStatus getContactNotificationStatus() {
        return contactNotificationStatus;
    }

    public void setContactNotificationStatus(ContactNotificationStatus contactNotificationStatus) {
        this.contactNotificationStatus = contactNotificationStatus;
    }

    public LocalDateTime getConfirmOwnershipDeadline() {
        return confirmOwnershipDeadline;
    }

    public void setConfirmOwnershipDeadline(LocalDateTime confirmOwnershipDeadline) {
        this.confirmOwnershipDeadline = confirmOwnershipDeadline;
    }

    public String getAiAssessment() {
        return aiAssessment;
    }

    public void setAiAssessment(String aiAssessment) {
        this.aiAssessment = aiAssessment;
    }
}

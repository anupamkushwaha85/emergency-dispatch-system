package com.emergency.emergency108.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "emergency_assignments")
public class EmergencyAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Which emergency
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "emergency_id")
    private Emergency emergency;

    // Which ambulance
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "ambulance_id")
    private Ambulance ambulance;

    /**
     * Which driver accepted and completed this assignment.
     * References the User table (role = DRIVER).
     * Set when driver accepts the assignment.
     */
    @Column(name = "driver_id")
    private Long driverId;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    private LocalDateTime acceptedAt;
    private LocalDateTime rejectedAt;
    private LocalDateTime completedAt;

    /**
     * When assignment was cancelled (system timeout or emergency cancelled).
     */
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    /**
     * Reason for cancellation: DRIVER_TIMEOUT, EMERGENCY_CANCELLED, etc.
     */
    @Column(name = "cancellation_reason")
    private String cancellationReason;

    /**
     * Destination hospital assigned when patient is picked up.
     * Selected automatically as the nearest hospital to patient location.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_hospital_id")
    private Hospital destinationHospital;

    @Column(nullable = true)
    private LocalDateTime responseDeadline;

    /**
     * Time taken by driver to respond (accept/reject) in seconds.
     * Used for analytics and monitoring driver response times.
     */
    @Column(name = "response_time_seconds")
    private Integer responseTimeSeconds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmergencyAssignmentStatus status;

    @Version
    @Column(nullable = false)
    private Long version;

    // ---- lifecycle hook ----

    // ---- getters & setters ----

    public Long getId() {
        return id;
    }

    public Emergency getEmergency() {
        return emergency;
    }

    public void setEmergency(Emergency emergency) {
        this.emergency = emergency;
    }

    public Ambulance getAmbulance() {
        return ambulance;
    }

    public void setAmbulance(Ambulance ambulance) {
        this.ambulance = ambulance;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public EmergencyAssignmentStatus getStatus() {
        return status;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(LocalDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public LocalDateTime getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(LocalDateTime rejectedAt) {
        this.rejectedAt = rejectedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public void setStatus(EmergencyAssignmentStatus status) {
        this.status = status;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public LocalDateTime getResponseDeadline() {
        return responseDeadline;
    }

    public void setResponseDeadline(LocalDateTime responseDeadline) {
        this.responseDeadline = responseDeadline;
    }

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public Integer getResponseTimeSeconds() {
        return responseTimeSeconds;
    }

    public void setResponseTimeSeconds(Integer responseTimeSeconds) {
        this.responseTimeSeconds = responseTimeSeconds;
    }

    public Hospital getDestinationHospital() {
        return destinationHospital;
    }

    public void setDestinationHospital(Hospital destinationHospital) {
        this.destinationHospital = destinationHospital;
    }
}

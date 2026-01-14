package com.hackathon.emergency108.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "emergency_assignments")
public class EmergencyAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;



    // Which emergency
    @ManyToOne(optional = false)
    @JoinColumn(name = "emergency_id")
    private Emergency emergency;

    // Which ambulance
    @ManyToOne(optional = false)
    @JoinColumn(name = "ambulance_id")
    private Ambulance ambulance;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    private LocalDateTime acceptedAt;
    private LocalDateTime rejectedAt;
    private LocalDateTime completedAt;



    @Column(nullable = true)
    private LocalDateTime responseDeadline;


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

    public  EmergencyAssignmentStatus getStatus() {
        return status;
    }

    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; }

    public LocalDateTime getRejectedAt() { return rejectedAt; }
    public void setRejectedAt(LocalDateTime rejectedAt) { this.rejectedAt = rejectedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }


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
}

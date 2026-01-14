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
}

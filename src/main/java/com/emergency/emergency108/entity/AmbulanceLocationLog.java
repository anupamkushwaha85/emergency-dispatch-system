package com.emergency.emergency108.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Tracks ambulance GPS location history during emergency trips.
 * Used for tracking, replay, and analytics.
 */
@Entity
@Table(
    name = "ambulance_location_logs",
    indexes = {
        @Index(name = "idx_ambulance_emergency", columnList = "ambulance_id, emergency_id"),
        @Index(name = "idx_emergency_ts", columnList = "emergency_id, ts")
    }
)
public class AmbulanceLocationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Ambulance ID
     */
    @Column(name = "ambulance_id", nullable = false)
    private Long ambulanceId;

    /**
     * Emergency ID (trip tracking)
     */
    @Column(name = "emergency_id", nullable = false)
    private Long emergencyId;

    /**
     * Latitude coordinate
     */
    @Column(name = "lat", nullable = false)
    private Double lat;

    /**
     * Longitude coordinate
     */
    @Column(name = "lng", nullable = false)
    private Double lng;

    /**
     * Timestamp of this location reading
     */
    @Column(name = "ts", nullable = false)
    private LocalDateTime ts;

    // Constructors
    public AmbulanceLocationLog() {
    }

    public AmbulanceLocationLog(Long ambulanceId, Long emergencyId, Double lat, Double lng) {
        this.ambulanceId = ambulanceId;
        this.emergencyId = emergencyId;
        this.lat = lat;
        this.lng = lng;
        this.ts = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (ts == null) {
            ts = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAmbulanceId() {
        return ambulanceId;
    }

    public void setAmbulanceId(Long ambulanceId) {
        this.ambulanceId = ambulanceId;
    }

    public Long getEmergencyId() {
        return emergencyId;
    }

    public void setEmergencyId(Long emergencyId) {
        this.emergencyId = emergencyId;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLng() {
        return lng;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }

    public LocalDateTime getTs() {
        return ts;
    }

    public void setTs(LocalDateTime ts) {
        this.ts = ts;
    }
}

package com.hackathon.emergency108.entity;

import jakarta.persistence.*;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Tracks which driver is currently operating which ambulance.
 * Represents a driver's work shift/session.
 * 
 * Business Rules:
 * - One driver can have only one ACTIVE session at a time
 * - One ambulance can have only one ACTIVE session at a time
 * - Driver must be VERIFIED to start a session
 * - Session automatically expires after 24 hours for safety
 */
@Entity
@Table(
    name = "driver_sessions",
    indexes = {
        @Index(name = "idx_driver_status", columnList = "driver_id, status"),
        @Index(name = "idx_ambulance_status", columnList = "ambulance_id, status"),
        @Index(name = "idx_status_started", columnList = "status, session_start_time")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_active_driver",
            columnNames = {"driver_id", "status"}
        ),
        @UniqueConstraint(
            name = "uk_active_ambulance", 
            columnNames = {"ambulance_id", "status"}
        )
    }
)
public class DriverSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    @Column(name = "ambulance_id", nullable = false)
    private Long ambulanceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DriverSessionStatus status;

    @Column(name = "session_start_time", nullable = false)
    private LocalDateTime sessionStartTime;

    @Column(name = "session_end_time")
    private LocalDateTime sessionEndTime;

    /**
     * Last known location of the driver's ambulance during this session.
     * Updated when driver calls PUT /api/driver/location
     */
    @Column(name = "current_lat")
    private Double currentLat;

    @Column(name = "current_lng")
    private Double currentLng;

    @Column(name = "location_updated_at")
    private LocalDateTime locationUpdatedAt;

    /**
     * Last GPS heartbeat timestamp from driver app.
     * Updated every 3-5 seconds when driver is active.
     * If no heartbeat for 30+ seconds, driver is considered OFFLINE.
     */
    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;

    /**
     * Total emergencies handled in this session
     */
    @Column(name = "emergencies_handled", nullable = false)
    private Integer emergenciesHandled = 0;

    /**
     * Optimistic locking for concurrent updates
     */
    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors

    public DriverSession() {
    }

    public DriverSession(Long driverId, Long ambulanceId) {
        this.driverId = driverId;
        this.ambulanceId = ambulanceId;
        this.status = DriverSessionStatus.ONLINE;
        this.sessionStartTime = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Lifecycle callbacks

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (sessionStartTime == null) {
            sessionStartTime = LocalDateTime.now();
        }
        if (emergenciesHandled == null) {
            emergenciesHandled = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Business methods

    /**
     * Mark driver as on trip (handling an emergency)
     */
    public void startTrip() {
        if (this.status != DriverSessionStatus.ONLINE) {
            throw new IllegalStateException(
                "Cannot start trip: driver is not ONLINE (current: " + this.status + ")"
            );
        }
        this.status = DriverSessionStatus.ON_TRIP;
        this.emergenciesHandled++;
    }

    /**
     * Mark driver as back online (trip completed)
     */
    public void endTrip() {
        if (this.status != DriverSessionStatus.ON_TRIP) {
            throw new IllegalStateException(
                "Cannot end trip: driver is not ON_TRIP (current: " + this.status + ")"
            );
        }
        this.status = DriverSessionStatus.ONLINE;
    }

    /**
     * End the driver's shift
     */
    public void endSession() {
        if (this.status == DriverSessionStatus.ON_TRIP) {
            throw new IllegalStateException(
                "Cannot end session: driver is currently ON_TRIP. Complete the trip first."
            );
        }
        this.status = DriverSessionStatus.OFFLINE;
        this.sessionEndTime = LocalDateTime.now();
    }

    /**
     * Update driver's current location
     */
    public void updateLocation(double lat, double lng) {
        this.currentLat = lat;
        this.currentLng = lng;
        this.locationUpdatedAt = LocalDateTime.now();
    }

    /**
     * Update heartbeat timestamp (called by driver app every 3-5 seconds)
     */
    public void updateHeartbeat() {
        this.lastHeartbeat = LocalDateTime.now();
    }

    /**
     * Check if driver's GPS heartbeat is stale (no update for 1 hour - testing mode).
     * Used by stale driver detection service to auto-mark drivers OFFLINE.
     * 
     * @return true if last heartbeat was more than 1 hour ago or never received
     */
    public boolean isStale() {
        if (lastHeartbeat == null) {
            return true; // No heartbeat ever received
        }
        
        long secondsSinceLastHeartbeat = Duration.between(lastHeartbeat, LocalDateTime.now()).getSeconds();
        return secondsSinceLastHeartbeat > 3600;
    }

    /**
     * Check if session is active (not ended)
     */
    public boolean isActive() {
        return this.status != DriverSessionStatus.OFFLINE && this.sessionEndTime == null;
    }

    /**
     * Check if driver is available to accept emergencies
     */
    public boolean isAvailable() {
        return this.status == DriverSessionStatus.ONLINE;
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

    public Long getAmbulanceId() {
        return ambulanceId;
    }

    public void setAmbulanceId(Long ambulanceId) {
        this.ambulanceId = ambulanceId;
    }

    public DriverSessionStatus getStatus() {
        return status;
    }

    public void setStatus(DriverSessionStatus status) {
        this.status = status;
    }

    public LocalDateTime getSessionStartTime() {
        return sessionStartTime;
    }

    public void setSessionStartTime(LocalDateTime sessionStartTime) {
        this.sessionStartTime = sessionStartTime;
    }

    public LocalDateTime getSessionEndTime() {
        return sessionEndTime;
    }

    public void setSessionEndTime(LocalDateTime sessionEndTime) {
        this.sessionEndTime = sessionEndTime;
    }

    public Double getCurrentLat() {
        return currentLat;
    }

    public void setCurrentLat(Double currentLat) {
        this.currentLat = currentLat;
    }

    public Double getCurrentLng() {
        return currentLng;
    }

    public void setCurrentLng(Double currentLng) {
        this.currentLng = currentLng;
    }

    public LocalDateTime getLocationUpdatedAt() {
        return locationUpdatedAt;
    }

    public void setLocationUpdatedAt(LocalDateTime locationUpdatedAt) {
        this.locationUpdatedAt = locationUpdatedAt;
    }

    public LocalDateTime getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(LocalDateTime lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    public Integer getEmergenciesHandled() {
        return emergenciesHandled;
    }

    public void setEmergenciesHandled(Integer emergenciesHandled) {
        this.emergenciesHandled = emergenciesHandled;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

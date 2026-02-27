package com.emergency.emergency108.dto;

import com.emergency.emergency108.entity.DriverSessionStatus;

/**
 * Broadcast payload for /topic/driver-status
 * Sent when a driver goes ONLINE, OFFLINE, or ON_TRIP.
 */
public class DriverStatusDTO {

    private Long driverId;
    private String driverName;
    private Long ambulanceId;
    private String licensePlate;
    private DriverSessionStatus status; // ONLINE, ON_TRIP, OFFLINE
    private Double latitude;
    private Double longitude;
    private java.time.LocalDateTime timestamp;

    public DriverStatusDTO() {
    }

    public DriverStatusDTO(Long driverId, String driverName, Long ambulanceId,
            String licensePlate, DriverSessionStatus status,
            Double latitude, Double longitude) {
        this.driverId = driverId;
        this.driverName = driverName;
        this.ambulanceId = ambulanceId;
        this.licensePlate = licensePlate;
        this.status = status;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = java.time.LocalDateTime.now();
    }

    // Getters & Setters
    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public Long getAmbulanceId() {
        return ambulanceId;
    }

    public void setAmbulanceId(Long ambulanceId) {
        this.ambulanceId = ambulanceId;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public DriverSessionStatus getStatus() {
        return status;
    }

    public void setStatus(DriverSessionStatus status) {
        this.status = status;
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

    public java.time.LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(java.time.LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}

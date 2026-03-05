package com.emergency.emergency108.dto;

/**
 * Broadcast payload for /topic/emergency-updates
 * Sent when an emergency is created, dispatched, accepted, completed, or
 * cancelled.
 */
/**
 * @author anupam kushwaha
 */
public class EmergencyUpdateDTO {

    private Long emergencyId;
    private String emergencyType;
    private String status; // CREATED, DISPATCHED, IN_PROGRESS, COMPLETED, CANCELLED, UNASSIGNED
    private Double latitude;
    private Double longitude;
    private Long assignedDriverId;
    private String assignedDriverName;
    private String event; // "CREATED", "DISPATCHED", "ACCEPTED", "COMPLETED", "CANCELLED"
    private java.time.LocalDateTime timestamp;

    public EmergencyUpdateDTO() {
    }

    public EmergencyUpdateDTO(Long emergencyId, String emergencyType, String status,
            Double latitude, Double longitude,
            Long assignedDriverId, String assignedDriverName, String event) {
        this.emergencyId = emergencyId;
        this.emergencyType = emergencyType;
        this.status = status;
        this.latitude = latitude;
        this.longitude = longitude;
        this.assignedDriverId = assignedDriverId;
        this.assignedDriverName = assignedDriverName;
        this.event = event;
        this.timestamp = java.time.LocalDateTime.now();
    }

    // Getters & Setters
    public Long getEmergencyId() {
        return emergencyId;
    }

    public void setEmergencyId(Long emergencyId) {
        this.emergencyId = emergencyId;
    }

    public String getEmergencyType() {
        return emergencyType;
    }

    public void setEmergencyType(String emergencyType) {
        this.emergencyType = emergencyType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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

    public Long getAssignedDriverId() {
        return assignedDriverId;
    }

    public void setAssignedDriverId(Long assignedDriverId) {
        this.assignedDriverId = assignedDriverId;
    }

    public String getAssignedDriverName() {
        return assignedDriverName;
    }

    public void setAssignedDriverName(String assignedDriverName) {
        this.assignedDriverName = assignedDriverName;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public java.time.LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(java.time.LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}

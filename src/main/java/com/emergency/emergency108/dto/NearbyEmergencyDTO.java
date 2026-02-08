package com.emergency.emergency108.dto;

/**
 * Privacy-focused DTO for Helping Hand feature.
 * Filters out sensitive data (phone, address, medical details).
 */
public class NearbyEmergencyDTO {

    private Long id;
    private String type; // Accident, Medical, etc.
    private double latitude;
    private double longitude;
    private double distanceKm;
    private String victimName; // First Name ONLY or "User nearby"
    private String status;

    public NearbyEmergencyDTO(Long id, String type, double latitude, double longitude, double distanceKm,
            String victimName, String status) {
        this.id = id;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distanceKm = distanceKm;
        this.victimName = victimName;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public String getVictimName() {
        return victimName;
    }

    public void setVictimName(String victimName) {
        this.victimName = victimName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

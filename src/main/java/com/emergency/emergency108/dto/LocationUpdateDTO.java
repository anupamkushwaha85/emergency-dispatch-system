package com.emergency.emergency108.dto;

import java.time.LocalDateTime;

/**
 * DTO broadcast via WebSocket whenever a driver's location changes.
 *
 * Published to:
 * /topic/live-locations (all-drivers feed for admin LiveMap)
 * /topic/driver/{driverId} (per-driver feed for targeted subscribers)
 *
 * The admin React panel subscribes to /topic/live-locations and uses
 * driverId to update only the changed ambulance marker on the map,
 * replacing the previous 10-second HTTP polling approach.
 */
/**
 * @author anupam kushwaha
 */
public class LocationUpdateDTO {

    private Long driverId;
    private Long ambulanceId;
    private double latitude;
    private double longitude;
    private LocalDateTime timestamp;

    public LocationUpdateDTO() {
    }

    public LocationUpdateDTO(Long driverId, Long ambulanceId, double latitude, double longitude,
            LocalDateTime timestamp) {
        this.driverId = driverId;
        this.ambulanceId = ambulanceId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}

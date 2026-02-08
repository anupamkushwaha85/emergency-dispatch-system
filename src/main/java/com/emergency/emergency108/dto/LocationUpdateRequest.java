package com.emergency.emergency108.dto;

/**
 * Request DTO for updating driver location
 */
public class LocationUpdateRequest {
    
    private double lat;
    private double lng;

    public LocationUpdateRequest() {
    }

    public LocationUpdateRequest(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }
}

package com.hackathon.emergency108.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ambulances")
public class Ambulance {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String code;

    @Column(name = "driver_name")
    private String driverName;

    @Column(name = "driver_phone")
    private String driverPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AmbulanceStatus status;

    @Column(name = "last_lat", nullable = false)
    private Double latitude;

    @Column(name = "last_lng", nullable = false)
    private Double longitude;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Ambulance type for fleet classification.
     * GOVERNMENT: Free service fleet (default)
     * PRIVATE: Paid service fleet
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "ambulance_type", nullable = false)
    private AmbulanceType ambulanceType = AmbulanceType.GOVERNMENT;

    /**
     * Base fare for PRIVATE ambulances.
     * NULL for GOVERNMENT ambulances.
     */
    @Column(name = "base_fare")
    private Double baseFare;

    /**
     * Per-kilometer rate for PRIVATE ambulances.
     * NULL for GOVERNMENT ambulances.
     */
    @Column(name = "per_km_rate")
    private Double perKmRate;

    @Version
    @Column(nullable = false)
    private Long version;


    // getters and setters

    /**
     * Update ambulance GPS location.
     */
    public void updateLocation(double lat, double lng) {
        this.latitude = lat;
        this.longitude = lng;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getDriverPhone() {
        return driverPhone;
    }

    public void setDriverPhone(String driverPhone) {
        this.driverPhone = driverPhone;
    }

    public AmbulanceStatus getStatus() {
        return status;
    }

    public void setStatus(AmbulanceStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public AmbulanceType getAmbulanceType() {
        return ambulanceType;
    }

    public void setAmbulanceType(AmbulanceType ambulanceType) {
        this.ambulanceType = ambulanceType;
    }

    public Double getBaseFare() {
        return baseFare;
    }

    public void setBaseFare(Double baseFare) {
        this.baseFare = baseFare;
    }

    public Double getPerKmRate() {
        return perKmRate;
    }

    public void setPerKmRate(Double perKmRate) {
        this.perKmRate = perKmRate;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}


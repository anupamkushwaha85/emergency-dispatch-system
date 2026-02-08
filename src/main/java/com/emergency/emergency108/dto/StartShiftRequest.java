package com.emergency.emergency108.dto;

/**
 * Request DTO for starting a driver shift
 */
public class StartShiftRequest {
    
    private Long ambulanceId;

    public StartShiftRequest() {
    }

    public StartShiftRequest(Long ambulanceId) {
        this.ambulanceId = ambulanceId;
    }

    public Long getAmbulanceId() {
        return ambulanceId;
    }

    public void setAmbulanceId(Long ambulanceId) {
        this.ambulanceId = ambulanceId;
    }
}

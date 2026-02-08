package com.emergency.emergency108.exception;

/**
 * Exception thrown when no ambulances are available for emergency dispatch.
 * This is a business logic exception indicating resource constraint.
 */
public class NoAmbulancesAvailableException extends RuntimeException {
    
    private final Long emergencyId;
    private final String reason;
    
    public NoAmbulancesAvailableException(String message) {
        super(message);
        this.emergencyId = null;
        this.reason = message;
    }
    
    public NoAmbulancesAvailableException(String message, Long emergencyId) {
        super(message);
        this.emergencyId = emergencyId;
        this.reason = message;
    }
    
    public NoAmbulancesAvailableException(String message, Long emergencyId, Throwable cause) {
        super(message, cause);
        this.emergencyId = emergencyId;
        this.reason = message;
    }
    
    public Long getEmergencyId() {
        return emergencyId;
    }
    
    public String getReason() {
        return reason;
    }
}

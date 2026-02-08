package com.emergency.emergency108.entity;

/**
 * Emergency source classification for hybrid payment model.
 * 
 * GOVERNMENT: Free emergency service (108 service) - No payment required
 * PRIVATE: Paid ambulance service (Uber-like) - Payment required
 * 
 * FUTURE: Can extend with CORPORATE, INSURANCE, etc.
 */
public enum EmergencySourceType {
    GOVERNMENT,  // Free emergency (108 government service)
    PRIVATE      // Paid emergency (private ambulance booking)
}

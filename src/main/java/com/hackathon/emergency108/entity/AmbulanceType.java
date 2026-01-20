package com.hackathon.emergency108.entity;

/**
 * Ambulance type classification for fleet management.
 * 
 * GOVERNMENT: Government-owned ambulances (free service fleet)
 * PRIVATE: Private ambulances (paid service fleet)
 * 
 * FUTURE: Can extend with ADVANCED_LIFE_SUPPORT, BASIC_LIFE_SUPPORT, NEONATAL, etc.
 */
public enum AmbulanceType {
    GOVERNMENT,  // Free service ambulances
    PRIVATE      // Private fleet (paid)
}

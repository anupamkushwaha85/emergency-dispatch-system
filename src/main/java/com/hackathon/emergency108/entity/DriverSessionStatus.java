package com.hackathon.emergency108.entity;

/**
 * Driver Session Status
 * 
 * ONLINE - Driver is available and can receive emergencies
 * ON_TRIP - Driver is currently handling an emergency
 * OFFLINE - Driver has ended shift or gone offline
 */
public enum DriverSessionStatus {
    ONLINE,
    ON_TRIP,
    OFFLINE
}

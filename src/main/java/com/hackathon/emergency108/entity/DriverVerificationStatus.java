package com.hackathon.emergency108.entity;

public enum DriverVerificationStatus {
    NOT_REQUIRED,   // For PUBLIC users
    PENDING,        // Driver registered, docs uploaded
    VERIFIED,       // Approved by admin
    REJECTED        // Rejected by admin
}

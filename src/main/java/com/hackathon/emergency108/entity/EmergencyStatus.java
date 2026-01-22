package com.hackathon.emergency108.entity;

/**
 * Emergency lifecycle states.
 * 
 * REAL-WORLD AMBULANCE FLOW:
 * CREATED → Patient presses SOS button
 * IN_PROGRESS → System processing, finding ambulance
 * DISPATCHED → Driver accepted, ambulance en route to patient
 * AT_PATIENT → Ambulance arrived at patient location
 * TO_HOSPITAL → Patient loaded, ambulance going to hospital
 * COMPLETED → Patient handed over to hospital
 * UNASSIGNED → No driver available or all rejected
 */
public enum EmergencyStatus {
    CREATED,        // SOS received
    IN_PROGRESS,    // System processing
    DISPATCHED,     // Driver accepted, en route to patient
    AT_PATIENT,     // Ambulance reached patient location
    TO_HOSPITAL,    // Patient loaded, going to hospital
    COMPLETED,      // Handover done
    CANCELLED,      // Cancelled by user
    UNASSIGNED      // Rejected/timeout
}

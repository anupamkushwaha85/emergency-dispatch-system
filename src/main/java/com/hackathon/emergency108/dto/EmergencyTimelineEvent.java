package com.hackathon.emergency108.dto;

import java.time.LocalDateTime;

public class EmergencyTimelineEvent {

    private String event;
    private LocalDateTime time;
    private String details;

    public EmergencyTimelineEvent(String event, LocalDateTime time, String details) {
        this.event = event;
        this.time = time;
        this.details = details;
    }

    public String getEvent() { return event; }
    public LocalDateTime getTime() { return time; }
    public String getDetails() { return details; }
}

package com.hackathon.emergency108.controller;

import com.hackathon.emergency108.system.SystemReadiness;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class SystemHealthController {

    private final SystemReadiness systemReadiness;

    public SystemHealthController(SystemReadiness systemReadiness) {
        this.systemReadiness = systemReadiness;
    }

    @GetMapping("/health/system")
    public Map<String, Object> systemHealth() {
        return Map.of(
                "ready", systemReadiness.isReady(),
                "status", systemReadiness.isReady() ? "READY" : "RECOVERING"
        );
    }
}

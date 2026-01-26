package com.hackathon.emergency108.controller;

import com.hackathon.emergency108.dto.LocationUpdateRequest;
import com.hackathon.emergency108.dto.NearbyEmergencyDTO;
import com.hackathon.emergency108.service.HelpingHandService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/helping-hand")
public class HelpingHandController {

    private final HelpingHandService helpingHandService;

    public HelpingHandController(HelpingHandService helpingHandService) {
        this.helpingHandService = helpingHandService;
    }

    /**
     * Update helper's location.
     * Called periodically by the app when in background/foreground.
     */
    @PostMapping("/location")
    public ResponseEntity<Void> updateLocation(
            @RequestParam Long userId,
            @RequestBody LocationUpdateRequest request) {
        helpingHandService.updateUserLocation(userId, request.getLat(), request.getLng());
        return ResponseEntity.ok().build();
    }

    /**
     * Get nearby emergencies.
     * Called periodically or on-demand.
     */
    @GetMapping("/nearby")
    public ResponseEntity<List<NearbyEmergencyDTO>> getNearbyEmergencies(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "3.0") double radiusKm) {
        List<NearbyEmergencyDTO> emergencies = helpingHandService.getNearbyEmergencies(userId, radiusKm);
        return ResponseEntity.ok(emergencies);
    }
}

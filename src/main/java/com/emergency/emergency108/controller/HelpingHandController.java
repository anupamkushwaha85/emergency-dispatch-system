package com.emergency.emergency108.controller;

import com.emergency.emergency108.dto.LocationUpdateRequest;
import com.emergency.emergency108.dto.NearbyEmergencyDTO;
import com.emergency.emergency108.service.HelpingHandService;
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

}

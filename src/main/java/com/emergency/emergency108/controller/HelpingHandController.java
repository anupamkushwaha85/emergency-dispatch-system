package com.emergency.emergency108.controller;

import com.emergency.emergency108.auth.guard.AuthGuard;
import com.emergency.emergency108.auth.security.AuthContext;
import com.emergency.emergency108.dto.LocationUpdateRequest;
import com.emergency.emergency108.dto.NearbyEmergencyDTO;
import com.emergency.emergency108.service.HelpingHandService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing HelpingHand endpoints.
 *
 * @author anupam kushwaha
 */
@RestController
@RequestMapping("/api/helping-hand")
public class HelpingHandController {

    private final HelpingHandService helpingHandService;
    private final AuthGuard authGuard;

    public HelpingHandController(HelpingHandService helpingHandService, AuthGuard authGuard) {
        this.helpingHandService = helpingHandService;
        this.authGuard = authGuard;
    }

    /**
     * Update helper's location.
     * Called periodically by the app when in background/foreground.
     */
    @PostMapping("/location")
    public ResponseEntity<Void> updateLocation(
            @RequestBody LocationUpdateRequest request) {
        authGuard.requireAuthenticated();
        Long userId = AuthContext.getUserId();
        helpingHandService.updateUserLocation(userId, request.getLat(), request.getLng());
        return ResponseEntity.ok().build();
    }

    /**
     * Fetch nearby emergencies for the logged-in helper.
     */
    @GetMapping("/nearby")
    public ResponseEntity<List<NearbyEmergencyDTO>> getNearbyEmergencies(
            @RequestParam(defaultValue = "3.0") double radiusKm) {
        authGuard.requireAuthenticated();
        Long userId = AuthContext.getUserId();
        List<NearbyEmergencyDTO> list = helpingHandService.getNearbyEmergencies(userId, radiusKm);
        return ResponseEntity.ok(list);
    }

}

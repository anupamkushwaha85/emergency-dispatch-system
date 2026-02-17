package com.emergency.emergency108.controller;

import com.emergency.emergency108.auth.token.AuthTokenPayload;
import com.emergency.emergency108.auth.token.TokenService;
import com.emergency.emergency108.entity.Ambulance;
import com.emergency.emergency108.entity.AmbulanceStatus;
import com.emergency.emergency108.entity.User;
import com.emergency.emergency108.entity.UserRole;
import com.emergency.emergency108.repository.AmbulanceRepository;
import com.emergency.emergency108.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ambulances")
public class AmbulanceController {

    private final AmbulanceRepository ambulanceRepository;
    private final UserRepository userRepository;
    private final TokenService tokenService;

    public AmbulanceController(AmbulanceRepository ambulanceRepository,
            UserRepository userRepository,
            TokenService tokenService) {
        this.ambulanceRepository = ambulanceRepository;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
    }

    @GetMapping
    public List<Ambulance> getAllAmbulances() {
        return ambulanceRepository.findAll();
    }

    /**
     * Create a new ambulance
     * POST /api/ambulances
     */
    @PostMapping
    public ResponseEntity<?> createAmbulance(@RequestHeader("Authorization") String authHeader,
            @RequestBody Ambulance ambulance) {
        try {
            validateAdmin(authHeader);

            ambulance.setUpdatedAt(LocalDateTime.now());
            // Default location (New Delhi center) if not provided
            if (ambulance.getLatitude() == null)
                ambulance.setLatitude(28.6139);
            if (ambulance.getLongitude() == null)
                ambulance.setLongitude(77.2090);
            if (ambulance.getStatus() == null)
                ambulance.setStatus(AmbulanceStatus.AVAILABLE);
            ambulance.setVersion(0L);

            Ambulance saved = ambulanceRepository.save(ambulance);
            return ResponseEntity.ok(saved);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create ambulance");
        }
    }

    /**
     * Assign a driver to an ambulance
     * PUT /api/ambulances/{id}/assign
     * Body: { "driverId": 123 }
     */
    @PutMapping("/{id}/assign")
    public ResponseEntity<?> assignDriver(@RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody Map<String, Long> request) {
        try {
            validateAdmin(authHeader);

            Long driverId = request.get("driverId");
            if (driverId == null)
                return ResponseEntity.badRequest().body("driverId is required");

            Ambulance ambulance = ambulanceRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Ambulance not found"));

            User driver = userRepository.findById(driverId)
                    .orElseThrow(() -> new RuntimeException("Driver not found"));

            if (driver.getRole() != UserRole.DRIVER) {
                return ResponseEntity.badRequest().body("User is not a driver");
            }

            // Update Ambulance details
            ambulance.setDriver(driver.getName());
            ambulance.setDriverPhone(driver.getPhone());
            ambulance.setUpdatedAt(LocalDateTime.now());

            ambulanceRepository.save(ambulance);

            return ResponseEntity.ok(ambulance);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to assign driver");
        }
    }

    private void validateAdmin(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        AuthTokenPayload payload = tokenService.validateAndParse(token);
        User user = userRepository.findById(payload.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getRole() != UserRole.ADMIN) {
            throw new RuntimeException("Only admins can perform this action");
        }
    }
}

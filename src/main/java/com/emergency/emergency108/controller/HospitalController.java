package com.emergency.emergency108.controller;

import com.emergency.emergency108.auth.token.AuthTokenPayload;
import com.emergency.emergency108.auth.token.TokenService;
import com.emergency.emergency108.entity.Hospital;
import com.emergency.emergency108.entity.User;
import com.emergency.emergency108.entity.UserRole;
import com.emergency.emergency108.repository.UserRepository;
import com.emergency.emergency108.service.HospitalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hospitals")
public class HospitalController {

    private static final Logger logger = LoggerFactory.getLogger(HospitalController.class);

    private final HospitalService hospitalService;
    private final TokenService tokenService;
    private final UserRepository userRepository;

    public HospitalController(HospitalService hospitalService, TokenService tokenService,
            UserRepository userRepository) {
        this.hospitalService = hospitalService;
        this.tokenService = tokenService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<?> getAllHospitals() {
        try {
            List<Hospital> hospitals = hospitalService.getAllHospitals();
            return ResponseEntity.ok(hospitals);
        } catch (Exception e) {
            logger.error("❌ Error fetching hospitals: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch hospitals");
        }
    }

    @PostMapping
    public ResponseEntity<?> addHospital(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Hospital hospital) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid Authorization header");
        }

        try {
            // Validate admin token
            String token = authHeader.substring(7);
            AuthTokenPayload payload = tokenService.validateAndParse(token);

            User admin = userRepository.findById(payload.getUserId()).orElse(null);

            if (admin == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Admin user not found");
            }

            if (admin.getRole() != UserRole.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Only admins can add hospitals");
            }

            // Input validation
            if (hospital.getName() == null || hospital.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Hospital name is required");
            }
            if (hospital.getLatitude() == null) {
                return ResponseEntity.badRequest().body("Hospital latitude is required");
            }
            if (hospital.getLongitude() == null) {
                return ResponseEntity.badRequest().body("Hospital longitude is required");
            }

            // Set default active status
            hospital.setIsActive(true);

            Hospital savedHospital = hospitalService.addHospital(hospital);
            logger.info("✅ Admin {} added new hospital: {}", admin.getId(), savedHospital.getName());

            return ResponseEntity.status(HttpStatus.CREATED).body(savedHospital);

        } catch (RuntimeException e) {
            logger.error("❌ Auth error adding hospital: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token");
        } catch (Exception e) {
            logger.error("❌ Error adding hospital: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to add hospital");
        }
    }
}

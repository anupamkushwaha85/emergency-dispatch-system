package com.emergency.emergency108.controller;

import com.emergency.emergency108.auth.guard.AuthGuard;
import com.emergency.emergency108.auth.security.AuthContext;
import com.emergency.emergency108.dto.DocumentUploadRequest;
import com.emergency.emergency108.entity.DriverVerificationStatus;
import com.emergency.emergency108.entity.User;
import com.emergency.emergency108.entity.UserRole;
import com.emergency.emergency108.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/driver")
public class DriverDocumentController {

    private final AuthGuard authGuard;
    private final UserRepository userRepository;

    public DriverDocumentController(AuthGuard authGuard, UserRepository userRepository) {
        this.authGuard = authGuard;
        this.userRepository = userRepository;
    }

    @PostMapping("/upload-document")
    public ResponseEntity<?> uploadDocument(@RequestBody DocumentUploadRequest request) {
        // 1. Ensure user is a DRIVER (Verified or Not)
        authGuard.requireRole(UserRole.DRIVER);

        Long driverId = AuthContext.get().getUserId();

        // 2. Validate Request
        if (request.getDocumentData() == null || request.getDocumentData().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document data is missing");
        }

        // 500KB limit ~ 700,000 Base64 characters
        if (request.getDocumentData().length() > 700000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document too large. Max 500KB.");
        }

        User user = userRepository.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // 3. Update User
        user.setDocumentUrl(request.getDocumentData());

        // If status was REJECTED or MISSING, set to PENDING because they uploaded a new
        // doc
        if (user.getDriverVerificationStatus() != DriverVerificationStatus.VERIFIED) {
            user.setDriverVerificationStatus(DriverVerificationStatus.PENDING);
        }

        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "Document uploaded successfully",
                "verificationStatus", user.getDriverVerificationStatus()));
    }
}

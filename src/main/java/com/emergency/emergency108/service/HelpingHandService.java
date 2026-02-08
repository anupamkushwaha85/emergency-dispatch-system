package com.emergency.emergency108.service;

import com.emergency.emergency108.dto.NearbyEmergencyDTO;
import com.emergency.emergency108.entity.*;
import com.emergency.emergency108.repository.EmergencyRepository;
import com.emergency.emergency108.repository.UserLocationRepository;
import com.emergency.emergency108.repository.UserRepository;
import com.emergency.emergency108.util.GeoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class HelpingHandService {

    private static final Logger log = LoggerFactory.getLogger(HelpingHandService.class);

    private final UserLocationRepository userLocationRepository;
    private final EmergencyRepository emergencyRepository;
    private final UserRepository userRepository;

    public HelpingHandService(UserLocationRepository userLocationRepository,
            EmergencyRepository emergencyRepository,
            UserRepository userRepository) {
        this.userLocationRepository = userLocationRepository;
        this.emergencyRepository = emergencyRepository;
        this.userRepository = userRepository;
    }

    /**
     * Update the location of a potential helper (PUBLIC user).
     */
    @Transactional
    public void updateUserLocation(Long userId, double lat, double lng) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Only PUBLIC users can be helpers
        if (user.getRole() != UserRole.PUBLIC) {
            return; // Drivers/Admins don't participate as loose helpers
        }

        UserLocation location = userLocationRepository.findByUser_Id(userId)
                .orElse(new UserLocation());

        if (location.getUser() == null) {
            location.setUser(user);
        }
        location.setLatitude(lat);
        location.setLongitude(lng);
        location.setLastUpdated(LocalDateTime.now());

        userLocationRepository.save(location);
    }

    /**
     * Get nearby active emergencies for a helper.
     * Enforces strict privacy and safety rules.
     */
    @Transactional(readOnly = true)
    public List<NearbyEmergencyDTO> getNearbyEmergencies(Long userId, double radiusKm) {
        log.info("🔍 Checking nearby emergencies for User: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // 1. Eligibility Check
        if (user.getRole() != UserRole.PUBLIC) {
            log.warn("❌ User {} is not PUBLIC (Role: {}), ineligible for Helping Hand.", userId, user.getRole());
            return Collections.emptyList();
        }

        // 2. Preference Check
        if (!user.isHelpingHandEnabled()) {
            log.info("🚫 User {} has Helping Hand disabled. Returning empty list.", userId);
            return Collections.emptyList();
        }

        // 3. Get User's last known location
        UserLocation userLoc = userLocationRepository.findByUser_Id(userId)
                .orElse(null);

        if (userLoc == null) {
            log.warn("⚠️ Location not found for User {}. Returning empty list.", userId);
            // Don't throw 500, just return empty list until location is updated
            return Collections.emptyList();
        }

        log.info("📍 User Location: Lat={}, Lng={}, LastUpdated={}", userLoc.getLatitude(), userLoc.getLongitude(),
                userLoc.getLastUpdated());

        // 3. Fetch Active Emergencies
        List<Emergency> activeEmergencies = emergencyRepository.findByStatusIn(
                Arrays.asList(EmergencyStatus.CREATED, EmergencyStatus.IN_PROGRESS, EmergencyStatus.DISPATCHED));

        log.info("🚑 Found {} potential active emergencies in DB.", activeEmergencies.size());

        // 4. Filter & Map
        List<NearbyEmergencyDTO> results = activeEmergencies.stream()
                .filter(e -> {
                    // Rule: Must be SELF (provenance check)
                    if (e.getEmergencyFor() != EmergencyFor.SELF) {
                        return false;
                    }

                    // Rule: Exclude own emergency
                    if (e.getUserId() != null && e.getUserId().equals(userId)) {
                        return false;
                    }

                    // Rule: Distance Check
                    double dist = GeoUtil.distanceKm(userLoc.getLatitude(), userLoc.getLongitude(), e.getLatitude(),
                            e.getLongitude());
                    if (dist > radiusKm) {
                        return false;
                    }

                    log.info("  ✅ Emergency {}: MATCHED! (Dist: {}km, Status: {})", e.getId(), dist, e.getStatus());
                    return true;
                })
                .sorted((e1, e2) -> {
                    double d1 = GeoUtil.distanceKm(userLoc.getLatitude(), userLoc.getLongitude(), e1.getLatitude(),
                            e1.getLongitude());
                    double d2 = GeoUtil.distanceKm(userLoc.getLatitude(), userLoc.getLongitude(), e2.getLatitude(),
                            e2.getLongitude());
                    return Double.compare(d1, d2);
                })
                .limit(5)
                .map(e -> {
                    double dist = GeoUtil.distanceKm(userLoc.getLatitude(), userLoc.getLongitude(), e.getLatitude(),
                            e.getLongitude());
                    String victimName = "User nearby";
                    if (e.getUserId() != null) {
                        Optional<User> victim = userRepository.findById(e.getUserId());
                        if (victim.isPresent() && victim.get().getName() != null) {
                            victimName = victim.get().getName().split(" ")[0];
                        }
                    }
                    return new NearbyEmergencyDTO(e.getId(), e.getType(), e.getLatitude(), e.getLongitude(), dist,
                            victimName, e.getStatus().toString());
                })
                .collect(Collectors.toList());

        log.info("✅ Returning {} nearby emergencies to user.", results.size());
        return results;
    }
}

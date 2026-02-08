package com.emergency.emergency108.service;

import com.emergency.emergency108.entity.Emergency;
import com.emergency.emergency108.entity.EmergencyStatus;
import com.emergency.emergency108.repository.EmergencyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * High-level emergency service for business logic.
 * Delegates dispatch to EmergencyDispatchService.
 */
@Service
public class EmergencyService {

    private static final Logger log = LoggerFactory.getLogger(EmergencyService.class);

    private final EmergencyRepository emergencyRepository;
    private final EmergencyDispatchService dispatchService;

    public EmergencyService(
            EmergencyRepository emergencyRepository,
            EmergencyDispatchService dispatchService) {
        this.emergencyRepository = emergencyRepository;
        this.dispatchService = dispatchService;
    }

    /**
     * Dispatch emergency to nearest VERIFIED + ONLINE driver.
     * Updates emergency status to DISPATCHED and creates assignment.
     * 
     * @param emergencyId Emergency ID
     */
    @Transactional
    public void dispatchToNearestDriver(Long emergencyId) {
        // Reload from DB to get latest status
        Emergency emergency = emergencyRepository.findById(emergencyId)
                .orElseThrow(() -> new IllegalArgumentException("Emergency not found: " + emergencyId));

        // STATUS CHECK: Must be CREATED before dispatch
        if (emergency.getStatus() != EmergencyStatus.CREATED) {
            log.warn("Cannot dispatch emergency {} - status is {} (must be CREATED)", 
                    emergencyId, emergency.getStatus());
            throw new IllegalStateException("Emergency must be in CREATED status to dispatch");
        }

        // Delegate to dispatch service
        dispatchService.dispatchToNearestAvailableAmbulance(emergencyId);
    }
}

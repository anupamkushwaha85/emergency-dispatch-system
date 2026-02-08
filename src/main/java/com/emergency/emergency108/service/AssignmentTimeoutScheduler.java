package com.emergency.emergency108.service;

import com.emergency.emergency108.resilience.DomainSafety;
import com.emergency.emergency108.system.SystemReadiness;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class AssignmentTimeoutScheduler {

    private final EmergencyAssignmentService assignmentService;
    private final SystemReadiness systemReadiness;


    private final AtomicBoolean running = new AtomicBoolean(false);

    public AssignmentTimeoutScheduler(
            EmergencyAssignmentService assignmentService,
            SystemReadiness systemReadiness
    ) {
        this.assignmentService = assignmentService;
        this.systemReadiness = systemReadiness;
    }

    @Scheduled(fixedRate = 30000)
    public void runTimeoutCheck() {

        // 🚫 Block scheduler until recovery finishes
        if (!systemReadiness.isReady()) {
            return; // private final SystemReadiness systemReadiness;

        }

        if (!running.compareAndSet(false, true)) {
            return;
        }

        try {
            DomainSafety.runSafely(
                    "ASSIGNMENT_TIMEOUT_SWEEP",
                    assignmentService::handleTimeouts
            );

        } finally {
            running.set(false);
        }
    }
}



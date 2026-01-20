package com.hackathon.emergency108.service;

import com.hackathon.emergency108.entity.Ambulance;
import com.hackathon.emergency108.entity.Emergency;
import com.hackathon.emergency108.entity.EmergencyStatus;
import com.hackathon.emergency108.event.DomainEventPublisher;
import com.hackathon.emergency108.event.EmergencyEvent;
import com.hackathon.emergency108.exception.NoAmbulancesAvailableException;
import com.hackathon.emergency108.metrics.DomainMetrics;
import com.hackathon.emergency108.repository.EmergencyRepository;
import com.hackathon.emergency108.resilience.DomainSafety;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * STEP 31: Automatic emergency dispatch after 60 seconds.
 * 
 * Behavior:
 * - When emergency is created, schedules auto-dispatch after 60 seconds
 * - If manually dispatched before 60 seconds, cancels scheduled task
 * - On app restart, recovers CREATED emergencies and dispatches if past deadline
 * 
 * Safety:
 * - Idempotent (checks status before dispatching)
 * - Crash-safe (startup recovery)
 * - No double dispatch (status guards)
 * - Service layer only (no auth required)
 */
@Service
public class AutoDispatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(AutoDispatchScheduler.class);

    private static final long AUTO_DISPATCH_DELAY_SECONDS = 60;

    private final TaskScheduler taskScheduler;
    private final EmergencyRepository emergencyRepository;
    private final EmergencyDispatchService emergencyDispatchService;
    private final EmergencyAssignmentService assignmentService;
    private final DomainEventPublisher eventPublisher;
    private final DomainMetrics metrics;
    
    // Self-injection for proper @Transactional proxy invocation
    private AutoDispatchScheduler self;

    // Track scheduled tasks for cancellation
    private final ConcurrentHashMap<Long, ScheduledFuture<?>> scheduledTasks = 
        new ConcurrentHashMap<>();

    public AutoDispatchScheduler(
            TaskScheduler taskScheduler,
            EmergencyRepository emergencyRepository,
            EmergencyDispatchService emergencyDispatchService,
            EmergencyAssignmentService assignmentService,
            DomainEventPublisher eventPublisher,
            DomainMetrics metrics
    ) {
        this.taskScheduler = taskScheduler;
        this.emergencyRepository = emergencyRepository;
        this.emergencyDispatchService = emergencyDispatchService;
        this.assignmentService = assignmentService;
        this.eventPublisher = eventPublisher;
        this.metrics = metrics;
    }
    
    /**
     * Self-injection setter - called by Spring after construction.
     * This allows calling @Transactional methods through the proxy.
     * @Lazy breaks circular dependency.
     */
    @org.springframework.beans.factory.annotation.Autowired
    public void setSelf(@org.springframework.context.annotation.Lazy AutoDispatchScheduler self) {
        this.self = self;
    }

    /**
     * Schedule auto-dispatch for an emergency after 60 seconds.
     * Called from EmergencyController.createEmergency().
     */
    public void scheduleAutoDispatch(Long emergencyId) {
        Instant triggerTime = Instant.now().plusSeconds(AUTO_DISPATCH_DELAY_SECONDS);

        ScheduledFuture<?> scheduledTask = taskScheduler.schedule(
                () -> {
                    try {
                        // Call through self-proxy to enable @Transactional
                        self.executeAutoDispatch(emergencyId);
                    } catch (Exception ex) {
                        log.error("Scheduled auto-dispatch failed for emergency {}: {}", 
                            emergencyId, ex.getMessage(), ex);
                    }
                },
                triggerTime
        );

        scheduledTasks.put(emergencyId, scheduledTask);

        log.debug(
                "Scheduled auto-dispatch for emergency {} at {}",
                emergencyId,
                triggerTime
        );
    }

    /**
     * Cancel scheduled auto-dispatch.
     * Called from EmergencyController.dispatch() when manual dispatch happens.
     */
    public void cancelScheduledDispatch(Long emergencyId) {
        ScheduledFuture<?> task = scheduledTasks.remove(emergencyId);

        if (task != null && !task.isDone()) {
            boolean cancelled = task.cancel(false);
            if (cancelled) {
                log.debug("Cancelled auto-dispatch for emergency {}", emergencyId);
            }
        }
    }

    /**
     * Execute auto-dispatch for an emergency.
     * Idempotent - only dispatches if still in CREATED state.
     */
    @Transactional
    public void executeAutoDispatch(Long emergencyId) {
        try {
            Emergency emergency = emergencyRepository.findById(emergencyId)
                    .orElse(null);

            // Guard: emergency doesn't exist
            if (emergency == null) {
                log.warn("Auto-dispatch: Emergency {} not found", emergencyId);
                scheduledTasks.remove(emergencyId);
                return;
            }

            // Guard: already dispatched (idempotent check)
            if (emergency.getStatus() != EmergencyStatus.CREATED) {
                log.debug(
                        "Auto-dispatch: Emergency {} already in state {}, skipping",
                        emergencyId,
                        emergency.getStatus()
                );
                scheduledTasks.remove(emergencyId);
                return;
            }

            log.info("Auto-dispatching emergency {} after 60 seconds timeout", emergencyId);

            // 1️⃣ Assign nearest ambulance FIRST (fail fast if none available)
            Ambulance ambulance;
            try {
                ambulance = emergencyDispatchService.assignNearestAmbulance(
                        emergency.getLatitude(),
                        emergency.getLongitude()
                );
            } catch (NoAmbulancesAvailableException ex) {
                log.warn("Auto-dispatch skipped for emergency {}: {}", emergencyId, ex.getMessage());
                throw ex; // Re-throw to be handled by outer catch
            }

            // 2️⃣ Mark emergency as IN_PROGRESS (only after ambulance found)
            assignmentService.markEmergencyInProgress(emergency);

            // 3️⃣ Create assignment
            assignmentService.assign(emergency, ambulance);

            // 4️⃣ Publish event
            eventPublisher.publish(
                    new EmergencyEvent(
                            emergencyId,
                            "AUTO_DISPATCH",
                            "Emergency auto-dispatched after 60 seconds"
                    )
            );

            // 5️⃣ Metrics
            metrics.dispatchSuccess();

            log.info(
                    "Auto-dispatch successful: emergency {} assigned to ambulance {}",
                    emergencyId,
                    ambulance.getCode()
            );

        } catch (NoAmbulancesAvailableException ex) {
            log.info("Auto-dispatch skipped for emergency {}: {}", emergencyId, ex.getMessage());
            
            // Keep emergency as CREATED (don't move to UNASSIGNED - user can still manually dispatch)
            // Just log and exit gracefully
            eventPublisher.publish(
                    new EmergencyEvent(
                            emergencyId,
                            "AUTO_DISPATCH_SKIPPED",
                            ex.getMessage()
                    )
            );
            
            // Remove from scheduled tasks and return
            scheduledTasks.remove(emergencyId);
            
        } catch (Exception ex) {
            log.error("Auto-dispatch failed for emergency {}: {}", emergencyId, ex.getMessage(), ex);
            
            // Only mark as UNASSIGNED if there was an actual assignment attempt
            // Check if emergency was moved to IN_PROGRESS (meaning assignment was attempted)
            try {
                Emergency emergency = emergencyRepository.findById(emergencyId).orElse(null);
                if (emergency != null && 
                    emergency.getStatus() == EmergencyStatus.IN_PROGRESS &&
                    !assignmentService.isAlreadyAssigned(emergencyId)) {
                    // Emergency was marked IN_PROGRESS but assignment failed
                    emergency.setStatus(EmergencyStatus.UNASSIGNED);
                    emergencyRepository.save(emergency);
                    
                    eventPublisher.publish(
                            new EmergencyEvent(
                                    emergencyId,
                                    "AUTO_DISPATCH_FAILED",
                                    "Auto-dispatch failed: " + ex.getMessage()
                            )
                    );
                }
            } catch (Exception innerEx) {
                log.error("Failed to handle emergency {} state after dispatch failure: {}", emergencyId, innerEx.getMessage());
            }
            
            // Remove from scheduled tasks
            scheduledTasks.remove(emergencyId);
        }
    }

    /**
     * CRASH RECOVERY: On application startup, recover emergencies that are still CREATED.
     * 
     * For each CREATED emergency:
     * - If past 60 seconds → dispatch immediately
     * - If within 60 seconds → schedule for remaining time
     */
    @EventListener(ApplicationReadyEvent.class)
    public void recoverPendingAutoDispatches() {
        log.info("Recovering pending auto-dispatches on startup...");

        List<Emergency> createdEmergencies = 
                emergencyRepository.findByStatus(EmergencyStatus.CREATED);

        if (createdEmergencies.isEmpty()) {
            log.info("No CREATED emergencies to recover");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        int immediateCount = 0;
        int scheduledCount = 0;

        for (Emergency emergency : createdEmergencies) {
            long elapsedSeconds = Duration.between(
                    emergency.getCreatedAt(),
                    now
            ).getSeconds();

            if (elapsedSeconds >= AUTO_DISPATCH_DELAY_SECONDS) {
                // Past deadline - dispatch immediately
                log.info(
                        "Emergency {} past 60 second deadline ({}s elapsed), dispatching now",
                        emergency.getId(),
                        elapsedSeconds
                );

                try {
                    // Call through self-proxy to enable @Transactional
                    self.executeAutoDispatch(emergency.getId());
                    immediateCount++;
                } catch (Exception ex) {
                    log.error("Recovery auto-dispatch failed for emergency {}: {}", 
                        emergency.getId(), ex.getMessage(), ex);
                }

            } else {
                // Still within deadline - schedule for remaining time
                long remainingSeconds = AUTO_DISPATCH_DELAY_SECONDS - elapsedSeconds;

                log.info(
                        "Emergency {} within deadline ({}s elapsed), scheduling in {}s",
                        emergency.getId(),
                        elapsedSeconds,
                        remainingSeconds
                );

                Instant triggerTime = Instant.now().plusSeconds(remainingSeconds);

                ScheduledFuture<?> scheduledTask = taskScheduler.schedule(
                        () -> {
                            try {
                                // Call through self-proxy to enable @Transactional
                                self.executeAutoDispatch(emergency.getId());
                            } catch (Exception ex) {
                                log.error("Scheduled auto-dispatch failed for emergency {}: {}", 
                                    emergency.getId(), ex.getMessage(), ex);
                            }
                        },
                        triggerTime
                );

                scheduledTasks.put(emergency.getId(), scheduledTask);
                scheduledCount++;
            }
        }

        log.info(
                "Auto-dispatch recovery complete: {} dispatched immediately, {} rescheduled",
                immediateCount,
                scheduledCount
        );
    }
}

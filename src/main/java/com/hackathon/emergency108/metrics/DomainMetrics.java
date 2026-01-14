package com.hackathon.emergency108.metrics;

import com.hackathon.emergency108.resilience.DomainSafety;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class DomainMetrics {

    private final Counter dispatchAttempt;
    private final Counter dispatchSuccess;
    private final Counter assignmentAccepted;
    private final Counter assignmentRejected;
    private final Counter assignmentTimeout;
    private final Counter startupRecovery;
    private final Counter assignmentCompleted;


    private final Timer dispatchTimer;
    private final Timer acceptTimer;

    public DomainMetrics(MeterRegistry registry) {

        this.dispatchAttempt =
                Counter.builder("emergency.dispatch.attempt")
                        .description("Dispatch attempts")
                        .register(registry);

        this.dispatchSuccess =
                Counter.builder("emergency.dispatch.success")
                        .description("Successful dispatches")
                        .register(registry);

        this.assignmentAccepted =
                Counter.builder("assignment.accepted")
                        .description("Assignments accepted")
                        .register(registry);

        this.assignmentRejected =
                Counter.builder("assignment.rejected")
                        .description("Assignments rejected")
                        .register(registry);

        this.assignmentTimeout =
                Counter.builder("assignment.timeout")
                        .description("Assignments timed out")
                        .register(registry);

        this.startupRecovery =
                Counter.builder("system.startup.recovery")
                        .description("Startup recovery executions")
                        .register(registry);

        this.dispatchTimer =
                Timer.builder("emergency.dispatch.time")
                        .description("Dispatch execution time")
                        .register(registry);

        this.acceptTimer =
                Timer.builder("assignment.accept.time")
                        .description("Assignment accept time")
                        .register(registry);

        this.assignmentCompleted =
                Counter.builder("assignment.completed")
                        .description("Number of completed assignments")
                        .register(registry);


    }

    // ---- counters ----

    public void dispatchAttempt() { DomainSafety.runSafely(
            "METRIC_DISPATCH_ATTEMPT",
            dispatchAttempt::increment
    ); }
    public void dispatchSuccess() { DomainSafety.runSafely(
            "METRIC_DISPATCH_SUCCESS",
            dispatchSuccess::increment
    ); }
    public void assignmentAccepted() { DomainSafety.runSafely(
            "METRIC_ASSIGNMENT_ACCEPTED",
            assignmentAccepted::increment
    ); }
    public void assignmentRejected() { DomainSafety.runSafely(
            "METRIC_ASSIGNMENT_REJECTED",
            assignmentRejected::increment
    ); }
    public void assignmentTimeout() { DomainSafety.runSafely(
            "METRIC_ASSIGNMENT_TIMEOUT",
            assignmentTimeout::increment
    ); }
    public void startupRecovery() { DomainSafety.runSafely(
            "METRIC_STARTUP_RECOVERY",
            startupRecovery::increment
    ); }
    public void assignmentCompleted() { DomainSafety.runSafely(
            "METRIC_ASSIGNMENT_COMPLETED",
            assignmentCompleted::increment
    );
    }


    // ---- timers ----

    public Timer.Sample startDispatchTimer() {
        return Timer.start();
    }

    public void stopDispatchTimer(Timer.Sample sample) {
        sample.stop(dispatchTimer);
    }

    public Timer.Sample startAcceptTimer() {
        return Timer.start();
    }

    public void stopAcceptTimer(Timer.Sample sample) {
        sample.stop(acceptTimer);
    }
}

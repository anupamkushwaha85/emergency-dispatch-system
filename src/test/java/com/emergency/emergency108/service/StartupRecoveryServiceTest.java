package com.emergency.emergency108.service;

import com.emergency.emergency108.entity.Ambulance;
import com.emergency.emergency108.entity.AmbulanceStatus;
import com.emergency.emergency108.entity.EmergencyAssignmentStatus;
import com.emergency.emergency108.event.DomainEventPublisher;
import com.emergency.emergency108.metrics.DomainMetrics;
import com.emergency.emergency108.repository.AmbulanceRepository;
import com.emergency.emergency108.repository.EmergencyAssignmentRepository;
import com.emergency.emergency108.repository.EmergencyRepository;
import com.emergency.emergency108.system.SystemReadiness;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class StartupRecoveryServiceTest {

    private EmergencyAssignmentRepository assignmentRepository;
    private AmbulanceRepository ambulanceRepository;
    private EmergencyRepository emergencyRepository;
    private SystemReadiness systemReadiness;
    private DomainEventPublisher eventPublisher;
    private DomainMetrics metrics;

    private StartupRecoveryService startupRecoveryService;

    @BeforeEach
    void setUp() {
        assignmentRepository = mock(EmergencyAssignmentRepository.class);
        ambulanceRepository = mock(AmbulanceRepository.class);
        emergencyRepository = mock(EmergencyRepository.class);
        systemReadiness = mock(SystemReadiness.class);
        eventPublisher = mock(DomainEventPublisher.class);
        metrics = mock(DomainMetrics.class);

        startupRecoveryService = new StartupRecoveryService(
                assignmentRepository,
                ambulanceRepository,
                emergencyRepository,
                systemReadiness,
                eventPublisher,
                metrics);

        when(assignmentRepository.findByStatusAndResponseDeadlineBefore(
                eq(EmergencyAssignmentStatus.ASSIGNED), any())).thenReturn(Collections.emptyList());
    }

    @Test
    void recoverSystemState_shouldReleaseBusyAmbulanceWithoutActiveAssignment() {
        Ambulance busyAmbulance = new Ambulance();
        ReflectionTestUtils.setField(busyAmbulance, "id", 77L);
        busyAmbulance.setStatus(AmbulanceStatus.BUSY);

        when(ambulanceRepository.findByStatus(AmbulanceStatus.BUSY)).thenReturn(List.of(busyAmbulance));
        when(assignmentRepository.existsByAmbulanceIdAndStatusIn(
                eq(77L),
                eq(EnumSet.of(EmergencyAssignmentStatus.ASSIGNED, EmergencyAssignmentStatus.ACCEPTED))))
                .thenReturn(false);

        startupRecoveryService.recoverSystemState();

        verify(ambulanceRepository).save(argThat(a ->
                a.getId().equals(77L) && a.getStatus() == AmbulanceStatus.AVAILABLE));
        verify(systemReadiness).markNotReady();
        verify(systemReadiness).markReady();
    }

    @Test
    void recoverSystemState_shouldKeepBusyAmbulanceWithActiveAssignment() {
        Ambulance busyAmbulance = new Ambulance();
        ReflectionTestUtils.setField(busyAmbulance, "id", 99L);
        busyAmbulance.setStatus(AmbulanceStatus.BUSY);

        when(ambulanceRepository.findByStatus(AmbulanceStatus.BUSY)).thenReturn(List.of(busyAmbulance));
        when(assignmentRepository.existsByAmbulanceIdAndStatusIn(
                eq(99L),
                eq(EnumSet.of(EmergencyAssignmentStatus.ASSIGNED, EmergencyAssignmentStatus.ACCEPTED))))
                .thenReturn(true);

        startupRecoveryService.recoverSystemState();

        verify(ambulanceRepository, never()).save(argThat(a ->
                a.getId().equals(99L) && a.getStatus() == AmbulanceStatus.AVAILABLE));
        verify(systemReadiness).markNotReady();
        verify(systemReadiness).markReady();
    }
}

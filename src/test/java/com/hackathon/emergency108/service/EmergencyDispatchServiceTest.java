package com.hackathon.emergency108.service;

import com.hackathon.emergency108.entity.*;
import com.hackathon.emergency108.event.DomainEvent;
import com.hackathon.emergency108.event.DomainEventPublisher;
import com.hackathon.emergency108.repository.AmbulanceRepository;
import com.hackathon.emergency108.repository.DriverSessionRepository;
import com.hackathon.emergency108.repository.EmergencyAssignmentRepository;
import com.hackathon.emergency108.repository.EmergencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class EmergencyDispatchServiceTest {

    private AmbulanceRepository ambulanceRepository;
    private DomainEventPublisher eventPublisher;
    private EmergencyRepository emergencyRepository;
    private EmergencyAssignmentRepository assignmentRepository;
    private DriverSessionRepository driverSessionRepository;

    private EmergencyDispatchService dispatchService;

    @BeforeEach
    void setUp() {
        // Manual mocking for interfaces (works better with Java 25 than @Mock on
        // classes)
        ambulanceRepository = mock(AmbulanceRepository.class);
        emergencyRepository = mock(EmergencyRepository.class);
        assignmentRepository = mock(EmergencyAssignmentRepository.class);
        driverSessionRepository = mock(DriverSessionRepository.class);

        // Manual stub for concrete class
        eventPublisher = new DomainEventPublisherStub();

        dispatchService = new EmergencyDispatchService(
                ambulanceRepository,
                eventPublisher,
                emergencyRepository,
                assignmentRepository,
                driverSessionRepository);
    }

    static class DomainEventPublisherStub extends DomainEventPublisher {
        public DomainEventPublisherStub() {
            super(null);
        }

        @Override
        public void publish(DomainEvent event) {
            // No-op
        }
    }

    @Test
    void testDispatch_ShouldExcludeRejectedDrivers() {
        // Arrange
        Long emergencyId = 1L;
        Long rejectedDriverId = 101L;
        Long acceptedDriverId = 102L;

        Emergency emergency = new Emergency();
        ReflectionTestUtils.setField(emergency, "id", emergencyId);
        emergency.setStatus(EmergencyStatus.CREATED);
        emergency.setLatitude(10.0);
        emergency.setLongitude(10.0);

        // Driver 1 (Rejected)
        DriverSession rejectedSession = new DriverSession();
        rejectedSession.setDriverId(rejectedDriverId);
        rejectedSession.setStatus(DriverSessionStatus.ONLINE);
        rejectedSession.setLastHeartbeat(LocalDateTime.now());
        rejectedSession.updateLocation(10.0, 10.0); // Very close

        // Driver 2 (Candidate)
        DriverSession acceptedSession = new DriverSession();
        acceptedSession.setDriverId(acceptedDriverId);
        acceptedSession.setStatus(DriverSessionStatus.ONLINE);
        acceptedSession.setLastHeartbeat(LocalDateTime.now());
        acceptedSession.updateLocation(10.1, 10.1); // Slightly further but available

        List<DriverSession> onlineSessions = new ArrayList<>();
        onlineSessions.add(rejectedSession);
        onlineSessions.add(acceptedSession);

        Ambulance ambulance = new Ambulance();
        ambulance.setId(500L);

        when(emergencyRepository.findById(emergencyId)).thenReturn(Optional.of(emergency));
        when(driverSessionRepository.findAllOnlineDrivers()).thenReturn(onlineSessions);

        // Mock that driver 101 has rejected this emergency
        when(assignmentRepository.findRejectedDriverIdsByEmergencyId(emergencyId))
                .thenReturn(List.of(rejectedDriverId));

        when(ambulanceRepository.findByDriverId(acceptedDriverId)).thenReturn(Optional.of(ambulance));

        // Act
        dispatchService.dispatchToNearestAvailableAmbulance(emergencyId);

        // Assert
        // Should assign to driver 102 (acceptedDriverId), NOT 101 (rejectedDriverId)
        verify(assignmentRepository).save(argThat(assignment -> assignment.getDriverId().equals(acceptedDriverId) &&
                assignment.getEmergency().getId().equals(emergencyId)));

        verify(emergencyRepository).save(argThat(e -> e.getStatus() == EmergencyStatus.DISPATCHED));
    }
}

package com.hackathon.emergency108.service;

import com.hackathon.emergency108.entity.*;
import com.hackathon.emergency108.repository.EmergencyAssignmentRepository;
import com.hackathon.emergency108.repository.EmergencyRepository;
import com.hackathon.emergency108.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmergencyCancellationServiceTest {

    @Mock
    private EmergencyRepository emergencyRepository;

    @Mock
    private EmergencyAssignmentRepository assignmentRepository;

    @Mock
    private UserRepository userRepository;

    private DriverSessionService driverSessionService; // Manual stub
    private EmergencyAuthorizationService authorizationService; // Manual stub

    private EmergencyCancellationService cancellationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Manual stubs
        driverSessionService = new DriverSessionServiceStub();
        authorizationService = new EmergencyAuthorizationServiceStub();

        cancellationService = new EmergencyCancellationService(
                emergencyRepository,
                assignmentRepository,
                userRepository,
                driverSessionService,
                authorizationService);
    }

    // Stub class for DriverSessionService
    static class DriverSessionServiceStub extends DriverSessionService {
        public DriverSessionServiceStub() {
            super(null, null, null, null);
        }

        @Override
        public DriverSession getActiveSession(Long driverId) {
            DriverSession session = new DriverSession();
            ReflectionTestUtils.setField(session, "id", 100L);
            session.setStatus(DriverSessionStatus.ON_TRIP);
            return session;
        }

        @Override
        public DriverSession saveSession(DriverSession session) {
            return session;
        }
    }

    // Stub class for EmergencyAuthorizationService
    static class EmergencyAuthorizationServiceStub extends EmergencyAuthorizationService {
        public EmergencyAuthorizationServiceStub() {
            super(null, null, null);
        }

        @Override
        public boolean canUserCancelEmergency(Long userId, Emergency emergency) {
            return true;
        }
    }

    @Test
    void testLateCancellation_ShouldSetStatusToCancelled() {
        // Arrange
        Long emergencyId = 1L;
        Long userId = 100L;
        Long driverId = 200L;

        Emergency emergency = new Emergency();
        ReflectionTestUtils.setField(emergency, "id", emergencyId);
        emergency.setStatus(EmergencyStatus.DISPATCHED);
        emergency.setConfirmationDeadline(LocalDateTime.now().minusMinutes(5)); // Past deadline (Late Cancellation)

        EmergencyAssignment assignment = new EmergencyAssignment();
        ReflectionTestUtils.setField(assignment, "id", 50L);
        assignment.setDriverId(driverId);
        assignment.setStatus(EmergencyAssignmentStatus.ASSIGNED);
        assignment.setEmergency(emergency);

        User user = new User();
        ReflectionTestUtils.setField(user, "id", userId);

        when(emergencyRepository.findById(emergencyId)).thenReturn(Optional.of(emergency));
        // Authorization service checks are stubbed to return true via the manual stub

        when(assignmentRepository.findByEmergencyIdAndStatus(emergencyId, EmergencyAssignmentStatus.ASSIGNED))
                .thenReturn(Optional.of(assignment));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        EmergencyCancellationService.CancellationResult result = cancellationService.cancelEmergency(emergencyId,
                userId);

        // Assert
        assertTrue(result.isSuccess());

        ArgumentCaptor<EmergencyAssignment> assignmentCaptor = ArgumentCaptor.forClass(EmergencyAssignment.class);
        verify(assignmentRepository).save(assignmentCaptor.capture());

        EmergencyAssignment savedAssignment = assignmentCaptor.getValue();
        assertEquals(EmergencyAssignmentStatus.CANCELLED, savedAssignment.getStatus(),
                "Status should be CANCELLED, not CANCELLED_BY_USER");
        assertEquals("User cancelled emergency after driver assigned", savedAssignment.getCancellationReason());
    }
}

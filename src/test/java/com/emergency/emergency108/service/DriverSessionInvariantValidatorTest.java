package com.emergency.emergency108.service;

import com.emergency.emergency108.entity.*;
import com.emergency.emergency108.repository.AmbulanceRepository;
import com.emergency.emergency108.repository.DriverSessionRepository;
import com.emergency.emergency108.repository.EmergencyAssignmentRepository;
import com.emergency.emergency108.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverSessionInvariantValidatorTest {

    @Mock
    private DriverSessionRepository sessionRepository;

    @Mock
    private EmergencyAssignmentRepository assignmentRepository;

    @Mock
    private AmbulanceRepository ambulanceRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DriverSessionInvariantValidator validator;

    private DriverSession activeSession;

    @BeforeEach
    void setUp() {
        activeSession = new DriverSession();
        activeSession.setId(1L);
        activeSession.setDriverId(100L);
        activeSession.setAmbulanceId(500L);
        activeSession.setStatus(DriverSessionStatus.ONLINE);
        activeSession.setSessionStartTime(LocalDateTime.now().minusMinutes(5));
        activeSession.setLastHeartbeat(LocalDateTime.now());
    }

    @Test
    void validateInvariants_AllValid() {
        // Arrange
        User driver = new User();
        driver.setId(100L);
        driver.setDriverVerificationStatus(DriverVerificationStatus.VERIFIED);

        Ambulance ambulance = new Ambulance();
        ambulance.setId(500L);

        when(sessionRepository.findActiveSessions()).thenReturn(List.of(activeSession));
        when(userRepository.findById(100L)).thenReturn(Optional.of(driver));
        when(ambulanceRepository.findById(500L)).thenReturn(Optional.of(ambulance));
        when(assignmentRepository.findByStatus(EmergencyAssignmentStatus.ACCEPTED))
                .thenReturn(Collections.emptyList());
        when(sessionRepository.findStaleSessions(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // Act
        validator.validateInvariants();

        // Assert
        verify(sessionRepository, atLeastOnce()).findActiveSessions();
        verify(userRepository, atLeastOnce()).findById(100L);
        verify(ambulanceRepository, atLeastOnce()).findById(500L);
    }

    @Test
    void validateInvariants_InvalidDriver() {
        // Arrange
        when(sessionRepository.findActiveSessions()).thenReturn(List.of(activeSession));
        when(userRepository.findById(100L)).thenReturn(Optional.empty()); // Driver missing

        // Mock rest to prevent generic null pointer failures if code order changes
        when(ambulanceRepository.findById(any())).thenReturn(Optional.of(new Ambulance()));
        when(assignmentRepository.findByStatus(any())).thenReturn(Collections.emptyList());
        when(sessionRepository.findStaleSessions(any())).thenReturn(Collections.emptyList());

        // Act
        validator.validateInvariants();

        // Assert
        verify(userRepository, atLeastOnce()).findById(100L);
    }

    @Test
    void validateInvariants_OnTripWithoutAssignment() {
        // Arrange
        activeSession.setStatus(DriverSessionStatus.ON_TRIP);

        User driver = new User();
        driver.setId(100L);
        driver.setDriverVerificationStatus(DriverVerificationStatus.VERIFIED);

        when(sessionRepository.findActiveSessions()).thenReturn(List.of(activeSession));
        when(userRepository.findById(100L)).thenReturn(Optional.of(driver));
        when(ambulanceRepository.findById(500L)).thenReturn(Optional.of(new Ambulance()));

        // No assignment returned
        when(assignmentRepository.findByStatus(EmergencyAssignmentStatus.ACCEPTED))
                .thenReturn(Collections.emptyList());

        when(sessionRepository.findStaleSessions(any())).thenReturn(Collections.emptyList());

        // Act
        validator.validateInvariants();

        // Assert
        verify(assignmentRepository).findByStatus(EmergencyAssignmentStatus.ACCEPTED);
    }

    @Test
    void validateInvariants_DuplicateSessionsForDriver() {
        // Arrange
        DriverSession activeSession2 = new DriverSession();
        activeSession2.setId(2L);
        activeSession2.setDriverId(100L);
        activeSession2.setAmbulanceId(501L);
        activeSession2.setStatus(DriverSessionStatus.ONLINE);

        when(sessionRepository.findActiveSessions()).thenReturn(List.of(activeSession, activeSession2));

        when(userRepository.findById(100L)).thenReturn(Optional.of(new User()));
        when(ambulanceRepository.findById(any())).thenReturn(Optional.of(new Ambulance()));
        when(assignmentRepository.findByStatus(any())).thenReturn(Collections.emptyList());
        when(sessionRepository.findStaleSessions(any())).thenReturn(Collections.emptyList());

        // Act
        validator.validateInvariants();

        // Assert
        verify(sessionRepository, atLeastOnce()).findActiveSessions();
    }
}

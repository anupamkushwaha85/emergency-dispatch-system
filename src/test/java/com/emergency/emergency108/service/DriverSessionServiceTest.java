package com.emergency.emergency108.service;

import com.emergency.emergency108.entity.Ambulance;
import com.emergency.emergency108.entity.DriverSession;
import com.emergency.emergency108.entity.DriverSessionStatus;
import com.emergency.emergency108.entity.User;
import com.emergency.emergency108.entity.UserRole;
import com.emergency.emergency108.metrics.DomainMetrics;
import com.emergency.emergency108.repository.AmbulanceRepository;
import com.emergency.emergency108.repository.DriverSessionRepository;
import com.emergency.emergency108.repository.UserRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverSessionServiceTest {

    @Mock
    private DriverSessionRepository sessionRepository;

    @Mock
    private AmbulanceRepository ambulanceRepository;

    @Mock
    private UserRepository userRepository;

    private DomainMetrics metrics;

    private DriverSessionService sessionService;

    private User driver;
    private Ambulance ambulance;
    private DriverSession activeSession;

    @BeforeEach
    void setUp() {
        metrics = new DomainMetrics(new SimpleMeterRegistry());
        sessionService = new DriverSessionService(sessionRepository, userRepository, ambulanceRepository, metrics);

        driver = new User();
        driver.setId(100L);
        driver.setRole(UserRole.DRIVER);
        driver.setDriverVerificationStatus(com.emergency.emergency108.entity.DriverVerificationStatus.VERIFIED);

        ambulance = new Ambulance();
        ambulance.setId(500L);
        ambulance.setDriver("Test Driver");
        ambulance.setStatus(com.emergency.emergency108.entity.AmbulanceStatus.AVAILABLE);

        activeSession = new DriverSession();
        activeSession.setId(1L);
        activeSession.setDriverId(100L);
        activeSession.setAmbulanceId(500L);
        activeSession.setStatus(DriverSessionStatus.ONLINE);
        activeSession.setSessionStartTime(LocalDateTime.now().minusHours(1));
        activeSession.setLastHeartbeat(LocalDateTime.now());
    }

    @Test
    void startShift_Success() {
        // Arrange
        when(userRepository.findById(100L)).thenReturn(Optional.of(driver));
        when(sessionRepository.findActiveSessionByDriverId(100L)).thenReturn(Optional.empty());
        when(ambulanceRepository.findById(500L)).thenReturn(Optional.of(ambulance));

        DriverSession newSession = new DriverSession();
        newSession.setId(2L);
        newSession.setDriverId(100L);
        newSession.setStatus(DriverSessionStatus.ONLINE);
        when(sessionRepository.save(any(DriverSession.class))).thenReturn(newSession);

        // Act
        DriverSession result = sessionService.startShift(100L, 500L);

        // Assert
        assertNotNull(result);
        assertEquals(DriverSessionStatus.ONLINE, result.getStatus());
        verify(sessionRepository).save(any(DriverSession.class));
    }

    @Test
    void startShift_FailsIfAlreadyActive() {
        // Arrange
        when(userRepository.findById(100L)).thenReturn(Optional.of(driver));
        when(sessionRepository.findActiveSessionByDriverId(100L)).thenReturn(Optional.of(activeSession));

        // Act & Assert
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            sessionService.startShift(100L, 500L);
        });
        assertEquals(
                "Driver already has an active session (ID: 1, Ambulance: 500, Status: ONLINE). End current session first.",
                exception.getMessage());
        verify(sessionRepository, never()).save(any(DriverSession.class));
    }

    @Test
    void endShift_Success() {
        // Arrange
        when(sessionRepository.findActiveSessionByDriverId(100L)).thenReturn(Optional.of(activeSession));

        // Act
        sessionService.endShift(100L);

        // Assert
        assertEquals(DriverSessionStatus.OFFLINE, activeSession.getStatus());
        assertNotNull(activeSession.getSessionEndTime());
        verify(sessionRepository).save(activeSession);
    }

    @Test
    void endShift_FailsIfOnTrip() {
        // Arrange
        activeSession.setStatus(DriverSessionStatus.ON_TRIP);
        when(sessionRepository.findActiveSessionByDriverId(100L)).thenReturn(Optional.of(activeSession));

        // Act & Assert
        Exception exception = assertThrows(IllegalStateException.class, () -> {
            sessionService.endShift(100L);
        });
        assertEquals("Cannot end shift while on an active trip. Complete the emergency first.", exception.getMessage());
        verify(sessionRepository, never()).save(any(DriverSession.class));
    }

    @Test
    void updateLocation_Success() {
        // Arrange
        when(sessionRepository.findActiveSessionByDriverId(100L)).thenReturn(Optional.of(activeSession));
        when(ambulanceRepository.findById(500L)).thenReturn(Optional.of(ambulance));

        // Act
        sessionService.updateLocation(100L, 28.6139, 77.2090);

        // Assert
        assertEquals(28.6139, activeSession.getCurrentLat());
        assertEquals(77.2090, activeSession.getCurrentLng());
        assertNotNull(activeSession.getLastHeartbeat());
        verify(sessionRepository).save(activeSession);
    }
}

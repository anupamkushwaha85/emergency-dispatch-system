package com.emergency.emergency108.controller;

import com.emergency.emergency108.auth.guard.AuthGuard;
import com.emergency.emergency108.auth.security.AuthContext;
import com.emergency.emergency108.entity.Emergency;
import com.emergency.emergency108.entity.EmergencyStatus;
import com.emergency.emergency108.metrics.DomainMetrics;
import com.emergency.emergency108.repository.AmbulanceRepository;
import com.emergency.emergency108.repository.EmergencyAssignmentRepository;
import com.emergency.emergency108.repository.EmergencyRepository;
import com.emergency.emergency108.repository.UserRepository;
import com.emergency.emergency108.auth.token.TokenService;
import com.emergency.emergency108.service.*;
import com.emergency.emergency108.system.SystemReadiness;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EmergencyController.class)
@AutoConfigureMockMvc(addFilters = false)
class EmergencyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmergencyRepository emergencyRepository;
    @MockBean
    private EmergencyDispatchService emergencyDispatchService;
    @MockBean
    private EmergencyAssignmentService assignmentService;
    @MockBean
    private EmergencyTimelineService emergencyTimelineService;
    @MockBean
    private EmergencyAssignmentRepository assignmentRepository;
    @MockBean
    private DriverSessionService driverSessionService;
    @MockBean
    private SystemReadiness systemReadiness;
    @MockBean
    private DomainMetrics metrics;
    @MockBean
    private AuthGuard authGuard;
    @MockBean
    private AmbulanceRepository ambulanceRepository;
    @MockBean
    private EmergencyAuthorizationService authorizationService;
    @MockBean
    private EmergencyCancellationService cancellationService;
    @MockBean
    private NotificationService notificationService;
    @MockBean
    private AiAssistanceService aiAssistanceService;
    @MockBean
    private HelpingHandService helpingHandService;
    @MockBean
    private FCMNotificationService fcmNotificationService;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        // Assume auth passes
        Mockito.doNothing().when(authGuard).requireAuthenticated();
    }

    @Test
    void testCreateEmergency_Success() throws Exception {
        Emergency emergency = new Emergency();
        emergency.setLatitude(12.9716);
        emergency.setLongitude(77.5946);
        emergency.setType("MEDICAL");

        Emergency savedEmergency = new Emergency();
        ReflectionTestUtils.setField(savedEmergency, "id", 10L);
        savedEmergency.setLatitude(12.9716);
        savedEmergency.setLongitude(77.5946);
        savedEmergency.setType("MEDICAL");
        savedEmergency.setStatus(EmergencyStatus.CREATED);

        when(emergencyRepository.save(any(Emergency.class))).thenReturn(savedEmergency);
        when(helpingHandService.findNearbyHelpers(any(Emergency.class), any(Double.class)))
                .thenReturn(new ArrayList<>());

        try (MockedStatic<AuthContext> authContext = Mockito.mockStatic(AuthContext.class)) {
            authContext.when(AuthContext::getUserId).thenReturn(100L);

            mockMvc.perform(post("/api/emergencies")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(emergency)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(10))
                    .andExpect(jsonPath("$.status").value("CREATED"))
                    .andExpect(jsonPath("$.type").value("MEDICAL"));
        }
    }

    @Test
    void testGetTimeline_Success() throws Exception {
        when(emergencyTimelineService.getTimeline(10L)).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/api/emergencies/10/timeline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}

package com.emergency.emergency108.controller;

import com.emergency.emergency108.auth.guard.AuthGuard;
import com.emergency.emergency108.auth.security.AuthContext;
import com.emergency.emergency108.auth.token.TokenService;
import com.emergency.emergency108.entity.Emergency;
import com.emergency.emergency108.entity.EmergencyAssignment;
import com.emergency.emergency108.entity.EmergencyStatus;
import com.emergency.emergency108.entity.Hospital;
import com.emergency.emergency108.repository.EmergencyAssignmentRepository;
import com.emergency.emergency108.repository.EmergencyRepository;
import com.emergency.emergency108.repository.HospitalRepository;
import com.emergency.emergency108.repository.UserRepository;
import com.emergency.emergency108.service.DriverSessionService;
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DriverController.class)
@AutoConfigureMockMvc(addFilters = false)
class DriverControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DriverSessionService sessionService;
    @MockBean
    private AuthGuard authGuard;
    @MockBean
    private HospitalRepository hospitalRepository;
    @MockBean
    private EmergencyAssignmentRepository assignmentRepository;
    @MockBean
    private EmergencyRepository emergencyRepository;
    @MockBean
    private TokenService tokenService;
    @MockBean
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        Mockito.doNothing().when(authGuard).requireVerifiedDriver();
    }

    @Test
    void testMarkPatientPickedUp_Success() throws Exception {
        Emergency emergency = new Emergency();
        emergency.setStatus(EmergencyStatus.AT_PATIENT);

        EmergencyAssignment assignment = new EmergencyAssignment();

        Hospital nearestHospital = new Hospital();
        nearestHospital.setId(10L);
        nearestHospital.setName("Central Hospital");
        nearestHospital.setAddress("123 Main St");
        nearestHospital.setLatitude(28.6139);
        nearestHospital.setLongitude(77.2090);

        when(emergencyRepository.findById(5L)).thenReturn(Optional.of(emergency));
        when(assignmentRepository.findTopByEmergencyIdOrderByAssignedAtDesc(5L)).thenReturn(Optional.of(assignment));
        when(hospitalRepository.findNearestHospitals(eq(28.6139), eq(77.2090), anyInt()))
                .thenReturn(Collections.singletonList(nearestHospital));

        try (MockedStatic<AuthContext> authContext = Mockito.mockStatic(AuthContext.class)) {
            authContext.when(AuthContext::getUserId).thenReturn(100L);

            Map<String, Object> req = new HashMap<>();
            req.put("emergencyId", 5);
            req.put("patientLat", 28.6139);
            req.put("patientLng", 77.2090);

            mockMvc.perform(post("/api/driver/mark-patient-picked-up")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Patient picked up, heading to hospital"))
                    .andExpect(jsonPath("$.hospital.id").value(10))
                    .andExpect(jsonPath("$.hospital.name").value("Central Hospital"));
        }
    }

    @Test
    void testCompleteMission_TooFar_Fails() throws Exception {
        Emergency emergency = new Emergency();
        emergency.setStatus(EmergencyStatus.TO_HOSPITAL);

        Hospital hospital = new Hospital();
        hospital.setName("Distant Hospital");
        // Distant coordinates
        hospital.setLatitude(20.0000);
        hospital.setLongitude(70.0000);

        EmergencyAssignment assignment = new EmergencyAssignment();
        assignment.setDestinationHospital(hospital);

        when(emergencyRepository.findById(5L)).thenReturn(Optional.of(emergency));
        when(assignmentRepository.findTopByEmergencyIdOrderByAssignedAtDesc(5L)).thenReturn(Optional.of(assignment));

        try (MockedStatic<AuthContext> authContext = Mockito.mockStatic(AuthContext.class)) {
            authContext.when(AuthContext::getUserId).thenReturn(100L);

            Map<String, Object> req = new HashMap<>();
            req.put("emergencyId", 5);
            // Current coordinates
            req.put("currentLat", 28.6139);
            req.put("currentLng", 77.2090);

            // Expect 403 Forbidden due to distance > 100 meters
            mockMvc.perform(post("/api/driver/complete-mission")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("Too far from hospital"));
        }
    }
}

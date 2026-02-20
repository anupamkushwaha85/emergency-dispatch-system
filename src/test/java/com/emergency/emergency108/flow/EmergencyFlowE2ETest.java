package com.emergency.emergency108.flow;

import com.emergency.emergency108.auth.guard.AuthGuard;
import com.emergency.emergency108.auth.security.AuthContext;
import com.emergency.emergency108.auth.security.AuthUserPrincipal;
import com.emergency.emergency108.entity.*;
import com.emergency.emergency108.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.sql.init.mode=never"
})
class EmergencyFlowE2ETest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private AmbulanceRepository ambulanceRepository;

        @Autowired
        private DriverSessionRepository sessionRepository;

        @Autowired
        private EmergencyRepository emergencyRepository;

        @Autowired
        private EmergencyAssignmentRepository assignmentRepository;

        @Autowired
        private HospitalRepository hospitalRepository;

        @MockBean
        private AuthGuard authGuard;

        private User patientUser;
        private User driverUser;
        private Ambulance ambulance;
        private Hospital hospital;

        @BeforeEach
        void setUp() {
                // Clear db
                sessionRepository.deleteAll();
                assignmentRepository.deleteAll();
                emergencyRepository.deleteAll();
                hospitalRepository.deleteAll();
                ambulanceRepository.deleteAll();
                userRepository.deleteAll();

                // 1. Create Patient
                patientUser = new User();
                patientUser.setPhone("1111111111");
                patientUser.setRole(UserRole.PUBLIC);
                patientUser.setDriverVerificationStatus(DriverVerificationStatus.PENDING);
                patientUser.setActive(true);
                patientUser = userRepository.save(patientUser);

                // 2. Create Driver
                driverUser = new User();
                driverUser.setPhone("2222222222");
                driverUser.setRole(UserRole.DRIVER);
                driverUser.setDriverVerificationStatus(DriverVerificationStatus.VERIFIED);
                driverUser.setActive(true);
                driverUser = userRepository.save(driverUser);

                // 3. Create Ambulance
                ambulance = new Ambulance();
                ambulance.setCode("E2E-AMB-01");
                ambulance.setLicensePlate("TEST-XX");
                ambulance.setStatus(AmbulanceStatus.AVAILABLE);
                ambulance.setLatitude(28.6139);
                ambulance.setLongitude(77.2090);
                ambulance = ambulanceRepository.save(ambulance);

                // 4. Create proper Session for Driver
                DriverSession session = new DriverSession();
                session.setDriverId(driverUser.getId());
                session.setAmbulanceId(ambulance.getId());
                session.setStatus(DriverSessionStatus.ONLINE);
                session.setSessionStartTime(LocalDateTime.now());
                session.setLastHeartbeat(LocalDateTime.now());
                session.setCurrentLat(28.6139);
                session.setCurrentLng(77.2090);
                sessionRepository.save(session);

                // 5. Create Hospital
                hospital = new Hospital();
                hospital.setName("E2E Test Hospital");
                hospital.setAddress("123 Test Street");
                hospital.setLatitude(28.6145);
                hospital.setLongitude(77.2100);
                hospital = hospitalRepository.save(hospital);

                // Mock AuthGuard bypass
                Mockito.doNothing().when(authGuard).requireAuthenticated();
                Mockito.doNothing().when(authGuard).requireVerifiedDriver();
        }

        @Test
        void testCompleteEmergencyLifecycle() throws Exception {
                Long emergencyId;

                // --- STEP 1: Patient Creates Emergency ---
                try (MockedStatic<AuthContext> authContext = Mockito.mockStatic(AuthContext.class)) {
                        authContext.when(AuthContext::getUserId).thenReturn(patientUser.getId());
                        authContext.when(AuthContext::get)
                                        .thenReturn(new AuthUserPrincipal(patientUser.getId(), UserRole.PUBLIC, false,
                                                        false));

                        Emergency req = new Emergency();
                        req.setType("HEART");
                        req.setSeverity("CRITICAL");
                        req.setLatitude(28.6140);
                        req.setLongitude(77.2095);
                        req.setEmergencyFor(EmergencyFor.SELF);

                        MvcResult createResult = mockMvc.perform(post("/api/emergencies")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(req)))
                                        .andExpect(status().isOk())
                                        .andReturn();

                        Emergency created = objectMapper.readValue(createResult.getResponse().getContentAsString(),
                                        Emergency.class);
                        emergencyId = created.getId();
                        assertThat(created.getStatus()).isEqualTo(EmergencyStatus.CREATED);

                        // Trigger Manual Dispatch (Bypass 100s wait)
                        mockMvc.perform(post("/api/emergencies/" + emergencyId + "/dispatch"))
                                        .andExpect(status().isOk());
                }

                // Verify Assignment Created in DB
                Optional<EmergencyAssignment> assignmentOpt = assignmentRepository
                                .findActiveAssignmentByEmergencyId(emergencyId);
                assertThat(assignmentOpt).isPresent();
                assertThat(assignmentOpt.get().getDriverId()).isEqualTo(driverUser.getId());

                // --- STEP 2: Driver Accepts Emergency ---
                try (MockedStatic<AuthContext> authContext = Mockito.mockStatic(AuthContext.class)) {
                        authContext.when(AuthContext::getUserId).thenReturn(driverUser.getId());
                        authContext.when(AuthContext::get)
                                        .thenReturn(new AuthUserPrincipal(driverUser.getId(), UserRole.DRIVER, false,
                                                        true));

                        mockMvc.perform(post("/api/driver/emergencies/" + emergencyId + "/accept"))
                                        .andExpect(status().isOk());

                        // Emergency Should be IN_PROGRESS
                        Emergency e1 = emergencyRepository.findById(emergencyId).get();
                        assertThat(e1.getStatus()).isEqualTo(EmergencyStatus.IN_PROGRESS);

                        // --- STEP 3: Driver Arrives at Patient ---
                        mockMvc.perform(post("/api/emergencies/" + emergencyId + "/arrive"))
                                        .andExpect(status().isOk());

                        Emergency e2 = emergencyRepository.findById(emergencyId).get();
                        assertThat(e2.getStatus()).isEqualTo(EmergencyStatus.AT_PATIENT);

                        // --- STEP 4: Pick Up Patient & Auto-assign Hospital ---
                        Map<String, Object> pickupReq = new HashMap<>();
                        pickupReq.put("emergencyId", emergencyId);
                        pickupReq.put("patientLat", 28.6140);
                        pickupReq.put("patientLng", 77.2095);

                        mockMvc.perform(post("/api/driver/mark-patient-picked-up")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(pickupReq)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.hospital.id").value(hospital.getId()));

                        Emergency e3 = emergencyRepository.findById(emergencyId).get();
                        assertThat(e3.getStatus()).isEqualTo(EmergencyStatus.TO_HOSPITAL);

                        // --- STEP 5: Complete Mission at Hospital ---
                        Map<String, Object> completeReq = new HashMap<>();
                        completeReq.put("emergencyId", emergencyId);
                        // Must be near the hospital (hospital is at 28.6145, 77.2100)
                        completeReq.put("currentLat", 28.6145);
                        completeReq.put("currentLng", 77.2100);

                        mockMvc.perform(post("/api/driver/complete-mission")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(completeReq)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.message")
                                                        .value("Mission completed successfully. Emergency closed."));

                        Emergency e4 = emergencyRepository.findById(emergencyId).get();
                        assertThat(e4.getStatus()).isEqualTo(EmergencyStatus.COMPLETED);
                }
        }
}

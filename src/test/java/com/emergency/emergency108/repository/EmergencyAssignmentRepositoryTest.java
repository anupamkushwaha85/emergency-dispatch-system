package com.emergency.emergency108.repository;

import com.emergency.emergency108.entity.Ambulance;
import com.emergency.emergency108.entity.AmbulanceStatus;
import com.emergency.emergency108.entity.Emergency;
import com.emergency.emergency108.entity.EmergencyAssignment;
import com.emergency.emergency108.entity.EmergencyAssignmentStatus;
import com.emergency.emergency108.entity.EmergencyStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
class EmergencyAssignmentRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EmergencyAssignmentRepository assignmentRepository;

    @Test
    void testFindActiveAssignmentByEmergencyId_Success() {
        Emergency emergency = new Emergency();
        emergency.setLatitude(28.0);
        emergency.setLongitude(77.0);
        emergency.setStatus(EmergencyStatus.DISPATCHED);
        emergency = entityManager.persistAndFlush(emergency);

        Ambulance ambulance = new Ambulance();
        ambulance.setCode("AMB-001");
        ambulance.setLicensePlate("DL-1234");
        ambulance.setLatitude(28.0);
        ambulance.setLongitude(77.0);
        ambulance.setStatus(AmbulanceStatus.AVAILABLE);
        ambulance = entityManager.persistAndFlush(ambulance);

        EmergencyAssignment assignment = new EmergencyAssignment();
        assignment.setEmergency(emergency);
        assignment.setAmbulance(ambulance);
        assignment.setDriverId(10L);
        assignment.setStatus(EmergencyAssignmentStatus.ASSIGNED);
        assignment.setAssignedAt(LocalDateTime.now());
        entityManager.persistAndFlush(assignment);

        Optional<EmergencyAssignment> activeAssignment = assignmentRepository
                .findActiveAssignmentByEmergencyId(emergency.getId());

        assertThat(activeAssignment).isPresent();
        assertThat(activeAssignment.get().getStatus()).isEqualTo(EmergencyAssignmentStatus.ASSIGNED);
    }

    @Test
    void testFindTimedOutAssignments_Success() {
        Emergency emergency = new Emergency();
        emergency.setLatitude(28.0);
        emergency.setLongitude(77.0);
        emergency.setStatus(EmergencyStatus.DISPATCHED);
        emergency = entityManager.persistAndFlush(emergency);

        Ambulance ambulance = new Ambulance();
        ambulance.setCode("AMB-002");
        ambulance.setLicensePlate("DL-9999");
        ambulance.setLatitude(28.0);
        ambulance.setLongitude(77.0);
        ambulance.setStatus(AmbulanceStatus.AVAILABLE);
        ambulance = entityManager.persistAndFlush(ambulance);

        EmergencyAssignment assignment1 = new EmergencyAssignment();
        assignment1.setEmergency(emergency);
        assignment1.setAmbulance(ambulance);
        assignment1.setDriverId(1L);
        assignment1.setStatus(EmergencyAssignmentStatus.ASSIGNED);
        assignment1.setAssignedAt(LocalDateTime.now().minusMinutes(5));
        assignment1.setResponseDeadline(LocalDateTime.now().minusMinutes(2)); // Deadline passed
        entityManager.persist(assignment1);

        EmergencyAssignment assignment2 = new EmergencyAssignment();
        assignment2.setEmergency(emergency);
        assignment2.setAmbulance(ambulance);
        assignment2.setDriverId(2L);
        assignment2.setStatus(EmergencyAssignmentStatus.ASSIGNED);
        assignment2.setAssignedAt(LocalDateTime.now());
        assignment2.setResponseDeadline(LocalDateTime.now().plusMinutes(2)); // Deadline in future
        entityManager.persist(assignment2);

        entityManager.flush();

        List<EmergencyAssignment> timedOut = assignmentRepository.findTimedOutAssignments(LocalDateTime.now());

        assertThat(timedOut).hasSize(1);
        assertThat(timedOut.get(0).getDriverId()).isEqualTo(1L);
    }
}

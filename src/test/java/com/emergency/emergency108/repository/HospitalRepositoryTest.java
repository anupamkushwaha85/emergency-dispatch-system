package com.emergency.emergency108.repository;

import com.emergency.emergency108.entity.Hospital;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
class HospitalRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Test
    void testFindNearestHospitals_NativeQueryHaversine_Success() {
        // Arrange
        Hospital farHospital = new Hospital();
        farHospital.setName("Distant General");
        // Distant coordinates
        farHospital.setLatitude(40.7128);
        farHospital.setLongitude(-74.0060);
        entityManager.persist(farHospital);

        Hospital nearHospital = new Hospital();
        nearHospital.setName("Near Clinic");
        // Near coordinates
        nearHospital.setLatitude(28.6140);
        nearHospital.setLongitude(77.2091);
        entityManager.persist(nearHospital);

        entityManager.flush();

        // Act
        // Patient location very close to nearHospital
        List<Hospital> nearestHospitals = hospitalRepository.findNearestHospitals(28.6139, 77.2090, 1);

        // Assert
        assertThat(nearestHospitals).isNotEmpty();
        assertThat(nearestHospitals).hasSize(1);
        assertThat(nearestHospitals.get(0).getName()).isEqualTo("Near Clinic");
    }
}

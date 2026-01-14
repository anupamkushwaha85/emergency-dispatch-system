package com.hackathon.emergency108.repository;

import com.hackathon.emergency108.entity.Ambulance;
import com.hackathon.emergency108.entity.AmbulanceStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AmbulanceRepository extends JpaRepository<Ambulance, Long> {

    List<Ambulance> findByStatus(AmbulanceStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
select a from Ambulance a
where a.status = 'AVAILABLE'
order by a.id
""")
    List<Ambulance> findAvailableForUpdate();

}

package com.emergency.emergency108.repository;

import com.emergency.emergency108.entity.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HospitalRepository extends JpaRepository<Hospital, Long> {

    List<Hospital> findByIsActiveTrue();

    /**
     * Find nearest hospitals using Haversine formula
     * Formula calculates great-circle distance between two points on a sphere
     */
    @Query(value = """
            SELECT h.*,
                   (6371 * acos(cos(radians(:latitude)) * cos(radians(h.latitude)) *
                    cos(radians(h.longitude) - radians(:longitude)) +
                    sin(radians(:latitude)) * sin(radians(h.latitude)))) AS distance
            FROM hospitals h
            WHERE h.is_active = true
            ORDER BY distance
            LIMIT :limit
            """, nativeQuery = true)
    List<Hospital> findNearestHospitals(
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude,
            @Param("limit") int limit);
}

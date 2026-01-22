package com.hackathon.emergency108.repository;

import com.hackathon.emergency108.entity.AmbulanceLocationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for AmbulanceLocationLog entity.
 * Handles database operations for ambulance GPS location tracking.
 */
@Repository
public interface AmbulanceLocationLogRepository extends JpaRepository<AmbulanceLocationLog, Long> {

    /**
     * Find all location logs for a specific ambulance.
     *
     * @param ambulanceId The ambulance ID
     * @return List of location logs ordered by timestamp
     */
    List<AmbulanceLocationLog> findByAmbulanceIdOrderByTsDesc(Long ambulanceId);

    /**
     * Find all location logs for a specific emergency trip.
     *
     * @param emergencyId The emergency ID
     * @return List of location logs ordered by timestamp
     */
    List<AmbulanceLocationLog> findByEmergencyIdOrderByTsAsc(Long emergencyId);

    /**
     * Find location logs for a specific ambulance and emergency.
     *
     * @param ambulanceId The ambulance ID
     * @param emergencyId The emergency ID
     * @return List of location logs ordered by timestamp
     */
    List<AmbulanceLocationLog> findByAmbulanceIdAndEmergencyIdOrderByTsAsc(Long ambulanceId, Long emergencyId);

    /**
     * Find location logs within a time range for an emergency.
     *
     * @param emergencyId The emergency ID
     * @param startTime   Start of time range
     * @param endTime     End of time range
     * @return List of location logs
     */
    List<AmbulanceLocationLog> findByEmergencyIdAndTsBetweenOrderByTsAsc(
            Long emergencyId,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    /**
     * Get the latest location for an ambulance during an emergency.
     *
     * @param ambulanceId The ambulance ID
     * @param emergencyId The emergency ID
     * @return The most recent location log
     */
    @Query("SELECT al FROM AmbulanceLocationLog al " +
           "WHERE al.ambulanceId = :ambulanceId AND al.emergencyId = :emergencyId " +
           "ORDER BY al.ts DESC LIMIT 1")
    AmbulanceLocationLog findLatestLocation(
            @Param("ambulanceId") Long ambulanceId,
            @Param("emergencyId") Long emergencyId
    );

    /**
     * Count location logs for an emergency.
     *
     * @param emergencyId The emergency ID
     * @return Number of location logs
     */
    long countByEmergencyId(Long emergencyId);

    /**
     * Delete all location logs for a specific emergency.
     *
     * @param emergencyId The emergency ID
     */
    @Transactional
    @Modifying
    void deleteByEmergencyId(Long emergencyId);

    /**
     * Delete old location logs (older than specified date).
     *
     * @param cutoffDate The cutoff date
     */
    @Transactional
    @Modifying
    void deleteByTsBefore(LocalDateTime cutoffDate);
}

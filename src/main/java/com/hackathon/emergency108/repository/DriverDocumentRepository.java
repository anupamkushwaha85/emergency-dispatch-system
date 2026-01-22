package com.hackathon.emergency108.repository;

import com.hackathon.emergency108.entity.DriverDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Repository interface for DriverDocument entity.
 * Handles database operations for driver verification documents.
 */
@Repository
public interface DriverDocumentRepository extends JpaRepository<DriverDocument, Long> {

    /**
     * Find driver documents by user ID (driver ID).
     *
     * @param userId The driver's user ID
     * @return Optional containing the driver document if found
     */
    Optional<DriverDocument> findByUserId(Long userId);

    /**
     * Check if a driver has submitted documents.
     *
     * @param userId The driver's user ID
     * @return true if documents exist, false otherwise
     */
    boolean existsByUserId(Long userId);

    /**
     * Find by license number.
     *
     * @param licenseNumber The driver's license number
     * @return Optional containing the driver document if found
     */
    Optional<DriverDocument> findByLicenseNumber(String licenseNumber);

    /**
     * Find by ID proof number.
     *
     * @param idProofNumber The driver's ID proof number
     * @return Optional containing the driver document if found
     */
    Optional<DriverDocument> findByIdProofNumber(String idProofNumber);

    /**
     * Delete driver documents by user ID.
     *
     * @param userId The driver's user ID
     */
    @Transactional
    @Modifying
    void deleteByUserId(Long userId);
}

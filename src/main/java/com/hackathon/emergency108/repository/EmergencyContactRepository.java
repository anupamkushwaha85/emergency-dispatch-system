package com.hackathon.emergency108.repository;

import com.hackathon.emergency108.entity.EmergencyContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Repository interface for EmergencyContact entity.
 * Handles database operations for user emergency contacts.
 */
@Repository
public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, Long> {

    /**
     * Find all emergency contacts for a specific user.
     *
     * @param userId The user ID
     * @return List of emergency contacts
     */
    List<EmergencyContact> findByUserId(Long userId);

    /**
     * Delete all emergency contacts for a specific user.
     *
     * @param userId The user ID
     */
    @Transactional
    @Modifying
    void deleteByUserId(Long userId);

    /**
     * Count emergency contacts for a specific user.
     *
     * @param userId The user ID
     * @return Number of emergency contacts
     */
    long countByUserId(Long userId);
}

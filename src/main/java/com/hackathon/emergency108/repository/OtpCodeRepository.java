package com.hackathon.emergency108.repository;

import com.hackathon.emergency108.entity.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for OtpCode entity.
 * Handles database operations for OTP authentication.
 */
@Repository
public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    /**
     * Find an OTP by identifier.
     *
     * @param identifier The unique OTP identifier (UUID)
     * @return Optional containing the OTP if found
     */
    Optional<OtpCode> findByIdentifier(String identifier);

    /**
     * Find the most recent OTP for a phone/email.
     *
     * @param phoneOrEmail The phone number or email
     * @return Optional containing the most recent OTP
     */
    @Query("SELECT o FROM OtpCode o WHERE o.phoneOrEmail = :phoneOrEmail ORDER BY o.id DESC LIMIT 1")
    Optional<OtpCode> findFirstByPhoneOrEmailOrderByIdDesc(@Param("phoneOrEmail") String phoneOrEmail);

    /**
     * Find all OTPs for a phone/email.
     *
     * @param phoneOrEmail The phone number or email
     * @return List of OTPs ordered by creation time (newest first)
     */
    @Query("SELECT o FROM OtpCode o WHERE o.phoneOrEmail = :phoneOrEmail ORDER BY o.id DESC")
    List<OtpCode> findByPhoneOrEmailOrderByIdDesc(@Param("phoneOrEmail") String phoneOrEmail);

    /**
     * Find unused OTPs for a phone/email.
     *
     * @param phoneOrEmail The phone number or email
     * @param used         Whether the OTP has been used
     * @return List of OTPs
     */
    @Query("SELECT o FROM OtpCode o WHERE o.phoneOrEmail = :phoneOrEmail AND o.used = :used")
    List<OtpCode> findByPhoneOrEmailAndUsed(@Param("phoneOrEmail") String phoneOrEmail, @Param("used") Boolean used);

    /**
     * Find by identifier and check if not used.
     *
     * @param identifier The unique OTP identifier
     * @param used       Whether the OTP has been used
     * @return Optional containing the OTP if found
     */
    Optional<OtpCode> findByIdentifierAndUsed(String identifier, Boolean used);

    /**
     * Check if an OTP exists by identifier.
     *
     * @param identifier The unique OTP identifier
     * @return true if exists, false otherwise
     */
    boolean existsByIdentifier(String identifier);

    /**
     * Delete expired OTPs (cleanup job).
     *
     * @param cutoffTime Delete OTPs that expired before this time
     */
    @Transactional
    @Modifying
    void deleteByExpiresAtBefore(LocalDateTime cutoffTime);

    /**
     * Delete all OTPs for a phone/email (cleanup).
     *
     * @param phoneOrEmail The phone number or email
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM OtpCode o WHERE o.phoneOrEmail = :phoneOrEmail")
    void deleteByPhoneOrEmail(@Param("phoneOrEmail") String phoneOrEmail);

    /**
     * Count unused OTPs for a phone/email (rate limiting).
     *
     * @param phoneOrEmail The phone number or email
     * @param used         Whether the OTP has been used
     * @return Number of unused OTPs
     */
    @Query("SELECT COUNT(o) FROM OtpCode o WHERE o.phoneOrEmail = :phoneOrEmail AND o.used = :used")
    long countByPhoneOrEmailAndUsed(@Param("phoneOrEmail") String phoneOrEmail, @Param("used") Boolean used);
}

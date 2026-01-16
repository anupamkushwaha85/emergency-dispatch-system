package com.hackathon.emergency108.auth.repository;

import com.hackathon.emergency108.auth.entity.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpCodeRepository
        extends JpaRepository<OtpCode, Long> {

    Optional<OtpCode> findTopByIdentifierAndUsedFalseOrderByExpiresAtDesc(
            String identifier
    );

}

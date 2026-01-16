package com.hackathon.emergency108.auth.service;

import com.hackathon.emergency108.auth.entity.OtpCode;
import com.hackathon.emergency108.auth.exception.InvalidOtpException;
import com.hackathon.emergency108.auth.repository.OtpCodeRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OtpService {

    private final OtpCodeRepository otpRepository;

    public OtpService(OtpCodeRepository otpRepository) {
        this.otpRepository = otpRepository;
    }

    public void sendOtp(String phoneOrEmail) {

        String code = String.valueOf(
                ThreadLocalRandom.current().nextInt(100000, 999999)
        );

        OtpCode otp = new OtpCode();
        otp.setIdentifier(phoneOrEmail);
        otp.setCode(code);
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        otpRepository.save(otp);

        // 🔥 TEMP: log instead of SMS/email
        System.out.println("OTP for " + phoneOrEmail + " = " + code);
    }

    public void verifyOtp(String phoneOrEmail, String code) {

        OtpCode otp = otpRepository
                .findTopByIdentifierAndUsedFalseOrderByExpiresAtDesc(phoneOrEmail)
                .orElseThrow(InvalidOtpException::new);

        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidOtpException();
        }

        if (!otp.getCode().equals(code)) {
            throw new InvalidOtpException();
        }

        otp.setUsed(true);
        otpRepository.save(otp);
    }
}


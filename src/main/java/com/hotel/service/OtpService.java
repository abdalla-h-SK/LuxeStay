package com.hotel.service;

import com.hotel.entity.Otp;
import com.hotel.exception.OtpException;
import com.hotel.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpRepository otpRepository;
    private final EmailService emailService;

    @Value("${otp.expiration-minutes:10}")
    private int expirationMinutes;

    private static final SecureRandom random = new SecureRandom();

    @Transactional
    public void generateAndSendOtp(String email, String purpose) {
        String code = String.format("%06d", random.nextInt(1_000_000));

        Otp otp = Otp.builder()
                .email(email)
                .otpCode(code)
                .purpose(purpose)
                .expirationTime(LocalDateTime.now().plusMinutes(expirationMinutes))
                .verified(false)
                .build();

        otpRepository.save(otp);
        emailService.sendOtpEmail(email, code, purpose);
        log.info("OTP generated for email: {} purpose: {}", email, purpose);
    }

    @Transactional
    public void verifyOtp(String email, String code, String purpose) {
        Otp otp = otpRepository.findLatestByEmailAndPurpose(email, purpose)
                .orElseThrow(() -> new OtpException("No OTP found. Please request a new one."));

        if (otp.isExpired()) {
            throw new OtpException("OTP has expired. Please request a new one.");
        }
        if (!otp.getOtpCode().equals(code)) {
            throw new OtpException("Invalid OTP code.");
        }

        otp.setVerified(true);
        otpRepository.save(otp);
        log.info("OTP verified for email: {} purpose: {}", email, purpose);
    }

    @Transactional
    public boolean isOtpVerified(String email, String purpose) {
        return otpRepository.findLatestByEmailAndPurpose(email, purpose)
                .map(Otp::isVerified)
                .orElse(false);
    }

    @Scheduled(fixedRate = 3_600_000) // every hour
    @Transactional
    public void cleanupExpiredOtps() {
        otpRepository.deleteExpiredOtps(LocalDateTime.now());
        log.debug("Cleaned up expired OTPs");
    }
}

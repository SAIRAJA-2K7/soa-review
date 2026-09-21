package com.soareview.backend.service;

import com.soareview.backend.entity.OtpEntity;
import com.soareview.backend.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${otp.ttl-seconds:300}")
    private int ttlSeconds;

    @Value("${otp.max-attempts:5}")
    private int maxAttempts;

    public String generateOtp(String email) {
        String code = String.format("%06d", new SecureRandom().nextInt(1000000));
        String codeHash = passwordEncoder.encode(code);

        Optional<OtpEntity> previous = otpRepository.findTopByEmailOrderByCreatedAtDesc(email);
        previous.ifPresent(otpRepository::delete);

        OtpEntity entity = OtpEntity.builder()
            .email(email)
            .codeHash(codeHash)
            .expiresAt(LocalDateTime.now().plusSeconds(ttlSeconds))
            .attempts(0)
            .used(false)
            .createdAt(LocalDateTime.now())
            .build();

        otpRepository.save(entity);
        return code;
    }

    public boolean validateOtp(String email, String otpInput) {
        OtpEntity otp = otpRepository.findTopByEmailOrderByCreatedAtDesc(email)
            .orElse(null);

        if (otp == null || otp.isUsed() || otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }

        if (otp.getAttempts() >= maxAttempts) {
            otpRepository.delete(otp);
            return false;
        }

        otp.setAttempts(otp.getAttempts() + 1);
        otpRepository.save(otp);

        boolean valid = passwordEncoder.matches(otpInput, otp.getCodeHash());
        if (valid) {
            otp.setUsed(true);
            otpRepository.save(otp);
        }

        return valid;
    }
}

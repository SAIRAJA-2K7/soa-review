package com.soareview.authservice.service;

import com.soareview.authservice.entity.OtpChallenge;
import com.soareview.authservice.repository.OtpChallengeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;

@Service
public class OtpChallengeStore {

    private final OtpChallengeRepository repository;
    private final long ttlSeconds;
    private final int maxAttempts;

    public OtpChallengeStore(
            OtpChallengeRepository repository,
            @Value("${otp.ttl-seconds:300}") long ttlSeconds,
            @Value("${otp.max-attempts:5}") int maxAttempts) {
        if (ttlSeconds < 1 || maxAttempts < 1) {
            throw new IllegalArgumentException("OTP TTL and maximum attempts must be positive");
        }
        this.repository = repository;
        this.ttlSeconds = ttlSeconds;
        this.maxAttempts = maxAttempts;
    }

    @Transactional
    public void issue(String email, String code) {
        String key = normalize(email);
        Instant expiry = Instant.now().plusSeconds(ttlSeconds);
        OtpChallenge challenge = repository.findForUpdate(key)
                .orElseGet(() -> new OtpChallenge(key, hash(code), expiry, 0));
        challenge.setCodeHash(hash(code));
        challenge.setExpiresAt(expiry);
        challenge.setFailedAttempts(0);
        repository.save(challenge);
    }

    @Transactional
    public boolean verifyAndConsume(String email, String suppliedCode) {
        Instant now = Instant.now();
        String key = normalize(email);
        OtpChallenge challenge = repository.findForUpdate(key).orElse(null);
        if (challenge == null) return false;
        if (!now.isBefore(challenge.getExpiresAt())) {
            repository.delete(challenge);
            return false;
        }
        if (suppliedCode != null && MessageDigest.isEqual(
                challenge.getCodeHash().getBytes(StandardCharsets.UTF_8),
                hash(suppliedCode).getBytes(StandardCharsets.UTF_8))) {
            repository.delete(challenge);
            return true;
        }
        int attempts = challenge.getFailedAttempts() + 1;
        if (attempts >= maxAttempts) {
            repository.delete(challenge);
        } else {
            challenge.setFailedAttempts(attempts);
            repository.save(challenge);
        }
        return false;
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String hash(String value) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}

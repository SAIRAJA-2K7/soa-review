package com.soareview.authservice.service;

import com.soareview.authservice.entity.AppUser;
import com.soareview.authservice.entity.PasswordResetCredential;
import com.soareview.authservice.repository.PasswordResetCredentialRepository;
import com.soareview.authservice.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PasswordResetService {

    public static final String GENERIC_REQUEST_MESSAGE =
            "If an account exists for that address, a password reset link will be sent.";
    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(15);
    private static final Duration REQUEST_INTERVAL = Duration.ofSeconds(60);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetCredentialRepository credentialRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private final String fromEmail;
    private final String resetPageUrl;
    private final ConcurrentHashMap<String, Instant> recentRequests = new ConcurrentHashMap<>();

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetCredentialRepository credentialRepository,
                                JavaMailSender mailSender,
                                PasswordEncoder passwordEncoder,
                                @Value("${mail.from:no-reply@talentsync.local}") String fromEmail,
                                @Value("${app.password-reset-url:http://localhost:5173/reset-password}") String resetPageUrl) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
        this.fromEmail = fromEmail;
        this.resetPageUrl = resetPageUrl;
    }

    @Transactional
    public String requestReset(String email) {
        String normalizedEmail = normalize(email);
        if (normalizedEmail.isBlank() || isRateLimited(normalizedEmail)) {
            return GENERIC_REQUEST_MESSAGE;
        }

        AppUser user = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (user == null || !user.isActive()) {
            return GENERIC_REQUEST_MESSAGE;
        }

        byte[] randomToken = new byte[32];
        SECURE_RANDOM.nextBytes(randomToken);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomToken);
        String tokenHash = sha256(rawToken);
        Instant expiry = Instant.now().plus(TOKEN_LIFETIME);
        credentialRepository.deleteOutstandingForUser(user.getId());
        credentialRepository.saveAndFlush(new PasswordResetCredential(tokenHash, user.getId(), expiry));

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject("TalentSync password reset");
            helper.setText("Use this link within 15 minutes to reset your password:\n\n" +
                    resetPageUrl + "?token=" + rawToken, false);
            mailSender.send(message);
        } catch (MailException | MessagingException exception) {
            credentialRepository.deleteById(tokenHash);
            log.warn("Password reset email delivery failed");
        }
        return GENERIC_REQUEST_MESSAGE;
    }

    @Transactional
    public void consume(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.isBlank() || newPassword == null || newPassword.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid reset request");
        }
        String tokenHash = sha256(rawToken);
        Instant now = Instant.now();
        PasswordResetCredential credential = credentialRepository.findForUpdate(tokenHash)
                .orElseThrow(() -> invalidCredential());
        if (credential.getConsumedAt() != null || !now.isBefore(credential.getExpiresAt())) {
            throw invalidCredential();
        }

        AppUser user = userRepository.findById(credential.getUserId())
                .orElseThrow(() -> invalidCredential());
        if (credentialRepository.consumeIfValid(tokenHash, now) != 1) {
            throw invalidCredential();
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        credentialRepository.deleteOutstandingForUser(user.getId());
    }

    private boolean isRateLimited(String normalizedEmail) {
        String key = sha256(normalizedEmail);
        Instant now = Instant.now();
        Instant previous = recentRequests.putIfAbsent(key, now);
        if (previous == null) return false;
        if (Duration.between(previous, now).compareTo(REQUEST_INTERVAL) < 0) return true;
        return recentRequests.replace(key, previous, now) == false;
    }

    private ResponseStatusException invalidCredential() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset link is invalid or expired");
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String sha256(String value) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}

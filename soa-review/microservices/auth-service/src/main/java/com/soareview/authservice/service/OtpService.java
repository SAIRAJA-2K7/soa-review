package com.soareview.authservice.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);

    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final long ttlSeconds;

    public OtpService(
            JavaMailSender mailSender,
            @Value("${mail.from:no-reply@talentsync.local}") String fromEmail,
            @Value("${otp.ttl-seconds:300}") long ttlSeconds) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
        this.ttlSeconds = ttlSeconds;
    }

    public String generateAndSend(String email) {
        String otp = generateOtp();
        sendOtp(email, otp);
        return otp;
    }

    public void sendOtp(String email, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("TalentSync verification code");
            helper.setText(
                    "Your TalentSync verification code is: " + otp + "\n\n" +
                            "Use this code to complete your sign in or password reset. This code is valid for " +
                            validityPeriod() + ".",
                    false
            );
            mailSender.send(message);
            log.info("OTP email sent");
        } catch (MailException | MessagingException ex) {
            log.warn("OTP email delivery failed");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "OTP delivery is unavailable", ex);
        }
    }

    private String validityPeriod() {
        if (ttlSeconds % 60 == 0) {
            long minutes = ttlSeconds / 60;
            return minutes + (minutes == 1 ? " minute" : " minutes");
        }
        return ttlSeconds + (ttlSeconds == 1 ? " second" : " seconds");
    }

    private String generateOtp() {
        int otp = new SecureRandom().nextInt(900_000) + 100_000;
        return String.valueOf(otp);
    }
}

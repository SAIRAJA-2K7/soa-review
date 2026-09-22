package com.soareview.authservice.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);

    private final JavaMailSender mailSender;
    private final String fromEmail;

    public OtpService(
            JavaMailSender mailSender,
            @Value("${mail.from:no-reply@talentsync.local}") String fromEmail) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
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
                            "Use this code to complete your sign in or password reset. This code is valid for 10 minutes.",
                    false
            );
            mailSender.send(message);
            log.info("OTP email sent to {} with code {}", email, otp);
        } catch (MailException | MessagingException ex) {
            log.warn("OTP email could not be sent to {}. Falling back to console log. Error: {}", email, ex.getMessage());
            log.info("OTP for {} is {}", email, otp);
        }
    }

    private String generateOtp() {
        int otp = new SecureRandom().nextInt(900_000) + 100_000;
        return String.valueOf(otp);
    }
}

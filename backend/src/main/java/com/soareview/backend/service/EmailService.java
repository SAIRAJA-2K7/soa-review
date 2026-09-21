package com.soareview.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendOtpEmail(String to, String otp) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("Your TalentSync OTP");
        msg.setText("Your one-time code is: " + otp + "\n\nThis code expires in 5 minutes.");

        try {
            mailSender.send(msg);
            log.info("OTP email sent to {}", to);
        } catch (Exception ex) {
            log.warn("SMTP unavailable for OTP delivery; fallback OTP is {} for {}", otp, to);
            System.out.println("[DEV OTP FALLBACK] Email: " + to + " | OTP: " + otp);
        }
    }
}

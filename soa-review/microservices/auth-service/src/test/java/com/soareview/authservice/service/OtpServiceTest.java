package com.soareview.authservice.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private MimeMessage mimeMessage;

    private OtpService otpService;

    @BeforeEach
    void setUp() {
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        otpService = new OtpService(javaMailSender, "no-reply@talentsync.local", 300);
    }

    @Test
    void sendOtp_shouldSendEmail() {
        otpService.sendOtp("candidate@example.com", "123456");

        verify(javaMailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendOtp_shouldReportDeliveryFailure() {
        doThrow(new MailSendException("smtp unavailable"))
                .when(javaMailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> otpService.sendOtp("candidate@example.com", "123456"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }
}

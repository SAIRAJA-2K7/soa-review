package com.soareview.authservice.service;

import com.soareview.authservice.entity.AppUser;
import com.soareview.authservice.repository.PasswordResetCredentialRepository;
import com.soareview.authservice.repository.UserRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

@SpringBootTest
class PasswordResetServiceIntegrationTest {

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetCredentialRepository credentialRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JavaMailSender mailSender;

    private AppUser user;

    @BeforeEach
    void setUp() throws Exception {
        reset(mailSender);
        when(mailSender.createMimeMessage()).thenReturn(
                new MimeMessage(Session.getInstance(new Properties())));
        user = new AppUser();
        user.setName("Reset Test User");
        user.setEmail("password-reset-" + UUID.randomUUID() + "@example.invalid");
        user.setPassword(passwordEncoder.encode("OriginalPassword123!"));
        user.setRole("USER");
        user.setVerified(true);
        user.setActive(true);
        user.setOnboarded(false);
        user = userRepository.saveAndFlush(user);
    }

    @Test
    void resetCredentialIsHashedExpiresAndCanOnlyBeConsumedOnce() throws Exception {
        assertThat(passwordResetService.requestReset(user.getEmail()))
                .isEqualTo(PasswordResetService.GENERIC_REQUEST_MESSAGE);
        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        String body = String.valueOf(messageCaptor.getValue().getContent());
        String token = extractToken(body);

        var credential = credentialRepository.findAll().stream()
                .filter(item -> item.getUserId().equals(user.getId()))
                .findFirst().orElseThrow();
        assertThat(credential.getTokenHash()).isNotEqualTo(token);
        assertThat(credential.getExpiresAt()).isAfter(Instant.now());
        assertThat(body).contains(token);

        passwordResetService.consume(token, "NewPassword123!");
        AppUser savedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("NewPassword123!", savedUser.getPassword())).isTrue();
        assertThat(passwordEncoder.matches("OriginalPassword123!", savedUser.getPassword())).isFalse();
        assertThat(credentialRepository.findById(credential.getTokenHash())).isEmpty();
        assertThatThrownBy(() -> passwordResetService.consume(token, "AnotherPassword123!"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void resetRequestResponseDoesNotRevealWhetherAccountExists() {
        String known = passwordResetService.requestReset(user.getEmail());
        String unknown = passwordResetService.requestReset("unknown-" + UUID.randomUUID() + "@example.invalid");
        assertThat(known).isEqualTo(unknown);
    }

    @Test
    void rateLimitsRepeatedRequestsWithoutChangingTheGenericResponse() {
        String first = passwordResetService.requestReset(user.getEmail());
        String second = passwordResetService.requestReset(user.getEmail());

        assertThat(first).isEqualTo(PasswordResetService.GENERIC_REQUEST_MESSAGE);
        assertThat(second).isEqualTo(first);
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void recruiterAccountCanUseTheSameResetFlow() throws Exception {
        user.setRole("ADMIN");
        userRepository.saveAndFlush(user);

        passwordResetService.requestReset(user.getEmail());
        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        String token = extractToken(String.valueOf(messageCaptor.getValue().getContent()));

        passwordResetService.consume(token, "RecruiterPassword123!");

        AppUser updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getRole()).isEqualTo("ADMIN");
        assertThat(passwordEncoder.matches("RecruiterPassword123!", updatedUser.getPassword())).isTrue();
    }

    @Test
    void expiredCredentialIsRejected() throws Exception {
        passwordResetService.requestReset(user.getEmail());
        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        String token = extractToken(String.valueOf(messageCaptor.getValue().getContent()));
        var credential = credentialRepository.findAll().stream()
                .filter(item -> item.getUserId().equals(user.getId()))
                .findFirst().orElseThrow();
        credentialRepository.updateExpiry(credential.getTokenHash(), Instant.now().minusSeconds(1));

        assertThatThrownBy(() -> passwordResetService.consume(token, "NewPassword123!"))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(passwordEncoder.matches("OriginalPassword123!",
                userRepository.findById(user.getId()).orElseThrow().getPassword())).isTrue();
    }

    @Test
    void concurrentConsumptionAllowsOnlyOnePasswordChange() throws Exception {
        passwordResetService.requestReset(user.getEmail());
        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        String token = extractToken(String.valueOf(messageCaptor.getValue().getContent()));
        Callable<Boolean> consume = () -> {
            try {
                passwordResetService.consume(token, "NewPassword123!");
                return true;
            } catch (ResponseStatusException exception) {
                return false;
            }
        };
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(consume);
            var second = executor.submit(consume);
            assertThat((int) (first.get() ? 1 : 0) + (second.get() ? 1 : 0)).isEqualTo(1);
        }
    }

    private String extractToken(String message) {
        Matcher matcher = Pattern.compile("[?&]token=([A-Za-z0-9_-]+)").matcher(message);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
}

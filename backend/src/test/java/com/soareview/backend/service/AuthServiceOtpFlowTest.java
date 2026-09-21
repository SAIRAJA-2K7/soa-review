package com.soareview.backend.service;

import com.soareview.backend.dto.AuthResponse;
import com.soareview.backend.dto.RegisterRequest;
import com.soareview.backend.entity.Role;
import com.soareview.backend.entity.UserEntity;
import com.soareview.backend.repository.UserRepository;
import com.soareview.backend.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceOtpFlowTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private OtpService otpService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerShouldCreateUnverifiedUser() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Alice Tester");
        request.setEmail("alice@example.com");
        request.setPassword("StrongPass123!");
        request.setRole("candidate");

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("StrongPass123!")).thenReturn("encoded-password");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.register(request);

        assertThat(response.isVerified()).isFalse();
        assertThat(response.isOnboarded()).isFalse();
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void verifyOtpShouldMarkUserAsVerified() {
        UserEntity user = UserEntity.builder()
            .id(42L)
            .name("Alice Tester")
            .email("alice@example.com")
            .password("encoded-password")
            .role(Role.USER)
            .verified(false)
            .onboarded(false)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        when(otpService.validateOtp("alice@example.com", "123456")).thenReturn(true);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(java.util.Optional.of(user));
        when(jwtTokenProvider.generateToken(any())).thenReturn("jwt-token");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.verifyOtp("alice@example.com", "123456");

        assertThat(response.isVerified()).isTrue();
        assertThat(user.isVerified()).isTrue();
        assertThat(response.getToken()).isEqualTo("jwt-token");
    }
}

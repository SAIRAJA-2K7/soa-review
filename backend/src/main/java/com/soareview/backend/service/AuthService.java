package com.soareview.backend.service;

import com.soareview.backend.dto.AuthResponse;
import com.soareview.backend.dto.LoginRequest;
import com.soareview.backend.dto.RegisterRequest;
import com.soareview.backend.entity.Role;
import com.soareview.backend.entity.UserEntity;
import com.soareview.backend.repository.UserRepository;
import com.soareview.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final OtpService otpService;
    private final EmailService emailService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail().trim())) {
            throw new IllegalArgumentException("User already exists");
        }

        Role mappedRole = switch (request.getRole().trim().toLowerCase()) {
            case "recruiter" -> Role.ADMIN;
            default -> Role.USER;
        };

        UserEntity user = UserEntity.builder()
            .name(request.getName().trim())
            .email(request.getEmail().trim().toLowerCase())
            .password(passwordEncoder.encode(request.getPassword()))
            .role(mappedRole)
            .verified(false)
            .onboarded(false)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        userRepository.save(user);
        return AuthResponse.builder()
            .email(user.getEmail())
            .role(user.getRole().name())
            .name(user.getName())
            .verified(user.isVerified())
            .onboarded(user.isOnboarded())
            .build();
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail().trim().toLowerCase(), request.getPassword())
        );

        String token = jwtTokenProvider.generateToken(authentication);
        UserEntity user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!user.isVerified()) {
            throw new IllegalArgumentException("Please verify your email before logging in");
        }

        return AuthResponse.builder()
            .token(token)
            .type("Bearer")
            .email(user.getEmail())
            .role(user.getRole().name())
            .name(user.getName())
            .verified(user.isVerified())
            .onboarded(user.isOnboarded())
            .build();
    }

    public String requestOtp(String email) {
        String normalized = email.trim().toLowerCase();
        userRepository.findByEmail(normalized)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String otp = otpService.generateOtp(normalized);
        emailService.sendOtpEmail(normalized, otp);
        return "OTP sent successfully";
    }

    public AuthResponse verifyOtp(String email, String otp) {
        if (!otpService.validateOtp(email.trim().toLowerCase(), otp)) {
            throw new IllegalArgumentException("Invalid or expired OTP");
        }

        UserEntity user = userRepository.findByEmail(email.trim().toLowerCase())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Authentication authentication = new UsernamePasswordAuthenticationToken(
            user.getEmail(),
            null,
            java.util.List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );

        user.setVerified(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtTokenProvider.generateToken(authentication);
        return AuthResponse.builder()
            .token(token)
            .type("Bearer")
            .email(user.getEmail())
            .role(user.getRole().name())
            .name(user.getName())
            .verified(user.isVerified())
            .onboarded(user.isOnboarded())
            .build();
    }

    public void resetPassword(String email, String otp, String password) {
        String normalized = email.trim().toLowerCase();
        if (!otpService.validateOtp(normalized, otp)) throw new IllegalArgumentException("Invalid or expired OTP");
        UserEntity user = userRepository.findByEmail(normalized).orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setPassword(passwordEncoder.encode(password));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }
}

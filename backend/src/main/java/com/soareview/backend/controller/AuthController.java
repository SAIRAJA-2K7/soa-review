package com.soareview.backend.controller;

import com.soareview.backend.dto.AuthResponse;
import com.soareview.backend.dto.LoginRequest;
import com.soareview.backend.dto.OtpRequest;
import com.soareview.backend.dto.RegisterRequest;
import com.soareview.backend.dto.VerifyOtpRequest;
import com.soareview.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/request-otp")
    public ResponseEntity<String> requestOtp(@Valid @RequestBody OtpRequest request) {
        return ResponseEntity.ok(authService.requestOtp(request.getEmail()));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request.getEmail(), request.getOtp()));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody java.util.Map<String, String> request) {
        authService.resetPassword(request.get("email"), request.get("otp"), request.get("password"));
        return ResponseEntity.ok("Password updated successfully");
    }
}

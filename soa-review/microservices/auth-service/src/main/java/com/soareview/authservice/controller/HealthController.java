package com.soareview.authservice.controller;

import com.soareview.authservice.config.JwtUtil;
import com.soareview.authservice.entity.AppUser;
import com.soareview.authservice.service.OtpService;
import com.soareview.authservice.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
public class HealthController {

    private static final Map<String, String> OTP_CODES = new ConcurrentHashMap<>();

    private final JwtUtil jwtUtil;
    private final OtpService otpService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public HealthController(JwtUtil jwtUtil, OtpService otpService, UserService userService, PasswordEncoder passwordEncoder) {
        this.jwtUtil = jwtUtil;
        this.otpService = otpService;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "auth-service");
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, Object> request) {
        String name = String.valueOf(request.getOrDefault("name", "")).trim();
        String email = String.valueOf(request.getOrDefault("email", "")).trim().toLowerCase();
        String password = String.valueOf(request.getOrDefault("password", ""));
        String role = String.valueOf(request.getOrDefault("role", "USER")).trim();

        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name, email and password are required");
        }

        AppUser user = userService.registerUser(name, email, password, role);
        return buildAuthResponse(user, true);
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, Object> request) {
        String email = String.valueOf(request.getOrDefault("email", "")).trim().toLowerCase();
        String password = String.valueOf(request.getOrDefault("password", ""));

        AppUser user = userService.findByEmail(email);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        if (!userService.isPasswordValid(password, user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        return buildAuthResponse(user, false);
    }

    @PostMapping("/request-otp")
    public Map<String, String> requestOtp(@RequestBody Map<String, String> request) {
        String email = String.valueOf(request.getOrDefault("email", "")).trim().toLowerCase();
        if (userService.findByEmail(email) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        String otp = otpService.generateAndSend(email);
        OTP_CODES.put(email, otp);
        return Map.of("message", "OTP sent successfully");
    }

    @PostMapping("/verify-otp")
    public Map<String, Object> verifyOtp(@RequestBody Map<String, String> request) {
        String email = String.valueOf(request.getOrDefault("email", "")).trim().toLowerCase();
        String otp = request.getOrDefault("otp", "");
        AppUser user = userService.findByEmail(email);

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found");
        }

        String expectedOtp = OTP_CODES.get(email);
        if (expectedOtp == null || !expectedOtp.equals(otp)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired OTP. Please request a new code.");
        }

        user.setVerified(true);
        userService.saveUser(user);
        OTP_CODES.remove(email);
        return buildAuthResponse(user, false);
    }

    @PostMapping("/reset-password")
    public Map<String, String> resetPassword(@RequestBody Map<String, String> request) {
        String email = String.valueOf(request.getOrDefault("email", "")).trim().toLowerCase();
        String otp = request.getOrDefault("otp", "");
        String password = request.getOrDefault("password", "");

        if (!otp.equals(OTP_CODES.get(email))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired OTP");
        }

        AppUser user = userService.findByEmail(email);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        user.setPassword(passwordEncoder.encode(password));
        userService.saveUser(user);
        OTP_CODES.remove(email);
        return Map.of("message", "Password updated successfully");
    }

    private Map<String, Object> buildAuthResponse(AppUser user, boolean includeTokenOnlyOnRegister) {
        String email = user.getEmail();
        String role = user.getRole();
        String token = jwtUtil.generateToken(email, role);

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("name", user.getName());
        response.put("email", email);
        response.put("role", role);
        response.put("verified", user.isVerified());
        response.put("onboarded", user.isOnboarded());
        response.put("token", token);
        response.put("type", "Bearer");
        return response;
    }
}

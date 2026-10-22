package com.soareview.authservice.controller;

import com.soareview.authservice.config.JwtUtil;
import com.soareview.authservice.entity.AppUser;
import com.soareview.authservice.service.OtpChallengeStore;
import com.soareview.authservice.service.OtpService;
import com.soareview.authservice.service.PasswordResetService;
import com.soareview.authservice.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class HealthController {

    private final JwtUtil jwtUtil;
    private final OtpService otpService;
    private final OtpChallengeStore otpChallengeStore;
    private final UserService userService;
    private final PasswordResetService passwordResetService;

    public HealthController(JwtUtil jwtUtil, OtpService otpService, OtpChallengeStore otpChallengeStore,
                            UserService userService, PasswordResetService passwordResetService) {
        this.jwtUtil = jwtUtil;
        this.otpService = otpService;
        this.otpChallengeStore = otpChallengeStore;
        this.userService = userService;
        this.passwordResetService = passwordResetService;
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
        String requestedRole = String.valueOf(request.getOrDefault("role", "USER")).trim();

        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name, email and password are required");
        }

        AppUser user = userService.registerUser(name, email, password, requestedRole);
        return buildAuthResponse(user);
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

        return buildAuthResponse(user);
    }

    @PostMapping("/request-otp")
    public Map<String, String> requestOtp(@RequestBody Map<String, String> request) {
        String email = String.valueOf(request.getOrDefault("email", "")).trim().toLowerCase();
        if (userService.findByEmail(email) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        String otp = otpService.generateAndSend(email);
        otpChallengeStore.issue(email, otp);
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

        if (!otpChallengeStore.verifyAndConsume(email, otp)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired OTP. Please request a new code.");
        }

        user.setVerified(true);
        userService.saveUser(user);
        return buildAuthResponse(user);
    }

    @PostMapping("/password-reset/request")
    public Map<String, String> requestPasswordReset(@RequestBody Map<String, String> request) {
        return Map.of("message", passwordResetService.requestReset(request.get("email")));
    }

    @PostMapping("/password-reset/consume")
    public Map<String, String> consumePasswordReset(@RequestBody Map<String, String> request) {
        passwordResetService.consume(request.get("token"), request.get("password"));
        return Map.of("message", "Password updated successfully");
    }

    private Map<String, Object> buildAuthResponse(AppUser user) {
        String email = user.getEmail();
        String role = user.getRole();
        String token = jwtUtil.generateToken(user.getId(), email, role, user.isVerified());

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("name", user.getName());
        response.put("email", email);
        response.put("role", role);
        response.put("requestedRole", user.getRequestedRole());
        response.put("verified", user.isVerified());
        response.put("onboarded", user.isOnboarded());
        response.put("token", token);
        response.put("type", "Bearer");
        return response;
    }
}

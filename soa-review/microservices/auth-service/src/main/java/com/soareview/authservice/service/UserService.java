package com.soareview.authservice.service;

import com.soareview.authservice.entity.AppUser;
import com.soareview.authservice.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AppUser registerUser(String name, String email, String password, String role) {
        String cleanEmail = (email == null ? "" : email).trim().toLowerCase(Locale.ROOT);
        String cleanName = (name == null ? "" : name).trim();
        String registrationRole = normalizeRegistrationRole(role);

        if (cleanEmail.isBlank() || cleanName.isBlank() || password == null || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name, email and password are required");
        }

        if (userRepository.existsByEmail(cleanEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already exists");
        }

        AppUser user = new AppUser();
        user.setName(cleanName);
        user.setEmail(cleanEmail);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(registrationRole);
        user.setRequestedRole(null);
        user.setVerified(false);
        user.setActive(true);
        user.setOnboarded(false);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public AppUser findByEmail(String email) {
        String cleanEmail = (email == null ? "" : email).trim().toLowerCase(Locale.ROOT);
        return userRepository.findByEmail(cleanEmail).orElse(null);
    }

    @Transactional
    public AppUser saveUser(AppUser user) {
        return userRepository.save(user);
    }

    @Transactional
    public void ensureDemoUsers(String candidateEmail, String candidatePassword,
                                String recruiterEmail, String recruiterPassword) {
        createDemoUserIfMissing(candidateEmail, candidatePassword, "USER");
        createDemoUserIfMissing(recruiterEmail, recruiterPassword, "RECRUITER");
    }

    private void createDemoUserIfMissing(String email, String password, String role) {
        if ((email == null || email.isBlank()) && (password == null || password.isBlank())) {
            return;
        }
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException("Both demo account email and password must be configured.");
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(normalizedEmail)) return;

        AppUser user = new AppUser();
        user.setName("TalentSync Demo " + role);
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setRequestedRole(null);
        user.setVerified(true);
        user.setActive(true);
        user.setOnboarded(false);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public AppUser getRequiredUser(String email) {
        AppUser user = findByEmail(email);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        return user;
    }

    @Transactional
    public boolean isPasswordValid(String rawPassword, AppUser user) {
        if (user == null || rawPassword == null || rawPassword.isBlank()) {
            return false;
        }

        String storedPassword = user.getPassword();
        if (storedPassword == null || storedPassword.isBlank()) {
            return false;
        }

        if (passwordEncoder.matches(rawPassword, storedPassword)) {
            return true;
        }

        if (Objects.equals(rawPassword, storedPassword)) {
            user.setPassword(passwordEncoder.encode(rawPassword));
            userRepository.save(user);
            return true;
        }

        return false;
    }

    private String normalizeRegistrationRole(String role) {
        if (role == null) return "USER";
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        return "RECRUITER".equals(normalized) ? "RECRUITER" : "USER";
    }

    public Map<String, Object> toProfileMap(AppUser user) {
        return Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "role", user.getRole(),
                "requestedRole", user.getRequestedRole() == null ? "" : user.getRequestedRole(),
                "verified", user.isVerified(),
                "onboarded", user.isOnboarded(),
                "professionalTitle", user.getProfessionalTitle() == null ? "" : user.getProfessionalTitle(),
                "location", user.getLocation() == null ? "" : user.getLocation(),
                "bio", user.getBio() == null ? "" : user.getBio()
        );
    }
}

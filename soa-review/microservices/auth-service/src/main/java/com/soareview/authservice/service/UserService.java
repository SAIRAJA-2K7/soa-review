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
import java.util.Optional;

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
        String normalizedRole = normalizeRole(role);

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
        user.setRole(normalizedRole);
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
    public void ensureDemoUser() {
        ensureDemoUsers();
    }

    @Transactional
    public void ensureDemoUsers() {
        createDemoUserIfMissing("candidate@demo.com", "TalentSync User", "USER", "Product Designer", "Remote",
                "Building the future of hiring.", "demo-password");
        createDemoUserIfMissing("recruiter@demo.com", "Sarah Chen", "ADMIN", "Talent Acquisition Lead", "New York, NY",
                "Scaling high-impact hiring programs.", "password123");
    }

    private void createDemoUserIfMissing(String email, String name, String role, String title, String location, String bio, String password) {
        Optional<AppUser> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            AppUser user = existing.get();
            boolean needsRefresh = false;

            if (!role.equals(user.getRole())) {
                user.setRole(role);
                needsRefresh = true;
            }
            if (!password.equals(user.getPassword()) && !passwordEncoder.matches(password, user.getPassword())) {
                user.setPassword(passwordEncoder.encode(password));
                needsRefresh = true;
            }
            if (user.getProfessionalTitle() == null || !user.getProfessionalTitle().equals(title)) {
                user.setProfessionalTitle(title);
                needsRefresh = true;
            }
            if (user.getLocation() == null || !user.getLocation().equals(location)) {
                user.setLocation(location);
                needsRefresh = true;
            }
            if (user.getBio() == null || !user.getBio().equals(bio)) {
                user.setBio(bio);
                needsRefresh = true;
            }
            if (!user.isVerified()) {
                user.setVerified(true);
                needsRefresh = true;
            }
            if (!user.isActive()) {
                user.setActive(true);
                needsRefresh = true;
            }

            if (needsRefresh) {
                userRepository.save(user);
            }
            return;
        }

        AppUser user = new AppUser();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setVerified(true);
        user.setActive(true);
        user.setOnboarded(false);
        user.setProfessionalTitle(title);
        user.setLocation(location);
        user.setBio(bio);
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

    public String normalizeRole(String role) {
        String normalized = role == null ? "USER" : role.trim().toUpperCase(Locale.ROOT);
        return "ADMIN".equals(normalized) ? "ADMIN" : "USER";
    }

    public Map<String, Object> toProfileMap(AppUser user) {
        return Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "role", user.getRole(),
                "verified", user.isVerified(),
                "onboarded", user.isOnboarded(),
                "professionalTitle", user.getProfessionalTitle() == null ? "" : user.getProfessionalTitle(),
                "location", user.getLocation() == null ? "" : user.getLocation(),
                "bio", user.getBio() == null ? "" : user.getBio()
        );
    }
}

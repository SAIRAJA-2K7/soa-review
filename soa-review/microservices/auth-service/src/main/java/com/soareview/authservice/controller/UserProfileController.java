package com.soareview.authservice.controller;

import com.soareview.authservice.entity.AppUser;
import com.soareview.authservice.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserProfileController {

    private final UserService userService;

    public UserProfileController(UserService userService) {
        this.userService = userService;
        this.userService.ensureDemoUser();
    }

    @GetMapping("/profile")
    public Map<String, Object> getProfile(@RequestHeader(value = "X-User-Email", required = false) String emailHeader) {
        String email = (emailHeader == null ? "" : emailHeader).trim().toLowerCase();
        AppUser user = userService.findByEmail(email);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("name", user.getName());
        profile.put("email", user.getEmail());
        profile.put("role", user.getRole());
        profile.put("verified", user.isVerified());
        profile.put("onboarded", user.isOnboarded());
        profile.put("professionalTitle", user.getProfessionalTitle());
        profile.put("location", user.getLocation());
        profile.put("bio", user.getBio());
        return profile;
    }

    @PatchMapping("/profile")
    public Map<String, Object> updateProfile(@RequestHeader(value = "X-User-Email", required = false) String emailHeader,
                                            @RequestBody Map<String, Object> request) {
        String email = (emailHeader == null ? "" : emailHeader).trim().toLowerCase();
        AppUser user = userService.findByEmail(email);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }

        if (request.get("name") != null) user.setName(String.valueOf(request.get("name")));
        if (request.get("professionalTitle") != null) user.setProfessionalTitle(String.valueOf(request.get("professionalTitle")));
        if (request.get("location") != null) user.setLocation(String.valueOf(request.get("location")));
        if (request.get("bio") != null) user.setBio(String.valueOf(request.get("bio")));
        if (request.get("onboarded") != null) user.setOnboarded(Boolean.parseBoolean(String.valueOf(request.get("onboarded"))));

        userService.saveUser(user);
        return getProfile(emailHeader);
    }
}

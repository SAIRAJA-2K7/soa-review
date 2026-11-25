package com.soareview.authservice.controller;

import com.soareview.authservice.entity.AppUser;
import com.soareview.authservice.entity.UserResume;
import com.soareview.authservice.repository.UserResumeRepository;
import com.soareview.authservice.service.UserService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserProfileController {

    private final UserService userService;
    private final UserResumeRepository resumeRepository;

    public UserProfileController(UserService userService, UserResumeRepository resumeRepository) {
        this.userService = userService;
        this.resumeRepository = resumeRepository;
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
        profile.put("requestedRole", user.getRequestedRole());
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

    @PutMapping(value = "/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadResume(@RequestHeader(value = "X-User-Email", required = false) String emailHeader,
                                            @RequestPart("file") MultipartFile file) {
        AppUser user = authenticatedUser(emailHeader);
        if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resume must be a non-empty PDF no larger than 5 MB");
        }
        if (!MediaType.APPLICATION_PDF_VALUE.equalsIgnoreCase(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Resume must be a PDF file");
        }

        try {
            byte[] content = file.getBytes();
            if (content.length < 5 || content[0] != '%' || content[1] != 'P'
                    || content[2] != 'D' || content[3] != 'F' || content[4] != '-') {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file is not a valid PDF");
            }

            String originalName = file.getOriginalFilename();
            String fileName = originalName == null ? "resume.pdf"
                    : originalName.replace('\\', '/').substring(originalName.replace('\\', '/').lastIndexOf('/') + 1);
            fileName = fileName.replaceAll("[\\p{Cntrl}]", "").trim();
            if (fileName.isBlank()) fileName = "resume.pdf";
            if (fileName.length() > 255) fileName = fileName.substring(fileName.length() - 255);

            UserResume resume = resumeRepository.findById(user.getId()).orElseGet(UserResume::new);
            resume.setUserId(user.getId());
            resume.setFileName(fileName);
            resume.setContentBase64(Base64.getEncoder().encodeToString(content));
            resumeRepository.save(resume);
            return Map.of("fileName", fileName, "size", content.length, "contentType", MediaType.APPLICATION_PDF_VALUE);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to read uploaded resume", exception);
        }
    }

    @GetMapping("/resume")
    public ResponseEntity<byte[]> downloadResume(
            @RequestHeader(value = "X-User-Email", required = false) String emailHeader) {
        AppUser user = authenticatedUser(emailHeader);
        UserResume resume = resumeRepository.findById(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resume not found"));
        byte[] content;
        try {
            content = Base64.getDecoder().decode(resume.getContentBase64());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Stored resume cannot be decoded", exception);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentLength(content.length);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(resume.getFileName(), StandardCharsets.UTF_8).build());
        return new ResponseEntity<>(content, headers, HttpStatus.OK);
    }

    @GetMapping("/resume/metadata")
    public ResponseEntity<Map<String, Object>> getResumeMetadata(
            @RequestHeader(value = "X-User-Email", required = false) String emailHeader) {
        AppUser user = authenticatedUser(emailHeader);
        return resumeRepository.findById(user.getId())
                .map(resume -> ResponseEntity.ok(Map.<String, Object>of(
                        "fileName", resume.getFileName(),
                        "size", Base64.getDecoder().decode(resume.getContentBase64()).length,
                        "contentType", MediaType.APPLICATION_PDF_VALUE)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @DeleteMapping("/resume")
    public ResponseEntity<Void> deleteResume(
            @RequestHeader(value = "X-User-Email", required = false) String emailHeader) {
        AppUser user = authenticatedUser(emailHeader);
        resumeRepository.deleteById(user.getId());
        return ResponseEntity.noContent().build();
    }

    private AppUser authenticatedUser(String emailHeader) {
        String email = (emailHeader == null ? "" : emailHeader).trim().toLowerCase();
        AppUser user = userService.findByEmail(email);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        return user;
    }
}

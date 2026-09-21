package com.soareview.backend.controller;

import com.soareview.backend.entity.UserEntity;
import com.soareview.backend.entity.JobEntity;
import com.soareview.backend.entity.SavedJobEntity;
import com.soareview.backend.dto.JobDto;
import com.soareview.backend.repository.InterviewRepository;
import com.soareview.backend.repository.JobRepository;
import com.soareview.backend.repository.SavedJobRepository;
import com.soareview.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final InterviewRepository interviewRepository;
    private final JobRepository jobRepository;
    private final SavedJobRepository savedJobRepository;

    @GetMapping("/dashboard")
    public Map<String, String> dashboard() {
        return Map.of("status", "user-dashboard-ready");
    }

    @GetMapping("/profile")
    public Map<String, Object> profile(org.springframework.security.core.Authentication authentication) {
        UserEntity user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        return Map.of("id", user.getId(), "name", user.getName(), "email", user.getEmail(),
            "role", user.getRole().name(), "verified", user.isVerified(), "onboarded", user.isOnboarded(),
            "avatar", user.getAvatar() == null ? "" : user.getAvatar(),
            "professionalTitle", user.getProfessionalTitle() == null ? "" : user.getProfessionalTitle(),
            "location", user.getLocation() == null ? "" : user.getLocation(),
            "bio", user.getBio() == null ? "" : user.getBio());
    }

    @PatchMapping("/profile")
    public Map<String, Object> updateProfile(@RequestBody Map<String, String> request, org.springframework.security.core.Authentication authentication) {
        UserEntity user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        if (request.containsKey("name")) user.setName(request.get("name").trim());
        if (request.containsKey("professionalTitle")) user.setProfessionalTitle(request.get("professionalTitle"));
        if (request.containsKey("location")) user.setLocation(request.get("location"));
        if (request.containsKey("bio")) user.setBio(request.get("bio"));
        if (request.containsKey("onboarded")) user.setOnboarded(Boolean.parseBoolean(request.get("onboarded")));
        user.setUpdatedAt(java.time.LocalDateTime.now());
        userRepository.save(user);
        return profile(authentication);
    }

    @DeleteMapping("/account")
    public void deactivateAccount(org.springframework.security.core.Authentication authentication) {
        UserEntity user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        user.setActive(false);
        user.setUpdatedAt(java.time.LocalDateTime.now());
        userRepository.save(user);
    }

    @GetMapping("/interviews")
    public Object interviews(org.springframework.security.core.Authentication authentication) {
        UserEntity candidate = userRepository.findByEmail(authentication.getName()).orElseThrow();
        return interviewRepository.findByCandidateOrderByScheduledAtAsc(candidate).stream().map(interview -> Map.of(
            "id", interview.getId(), "candidate", candidate.getName(), "job", interview.getJob().getTitle(),
            "date", interview.getScheduledAt().toLocalDate().toString(), "time", interview.getScheduledAt().toLocalTime().toString(),
            "type", interview.getType(), "mode", interview.getMode(), "status", interview.getStatus().name().toLowerCase(),
            "interviewer", interview.getInterviewer())).toList();
    }

    @GetMapping("/saved-jobs")
    public List<JobDto> savedJobs(org.springframework.security.core.Authentication authentication) {
        UserEntity user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        return savedJobRepository.findByUser(user).stream().map(saved -> toJobDto(saved.getJob())).toList();
    }

    @PostMapping("/saved-jobs/{jobId}")
    public void saveJob(@PathVariable Long jobId, org.springframework.security.core.Authentication authentication) {
        UserEntity user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        JobEntity job = jobRepository.findById(jobId).orElseThrow();
        if (savedJobRepository.findByUserAndJob(user, job).isEmpty()) {
            savedJobRepository.save(SavedJobEntity.builder().user(user).job(job).build());
        }
    }

    @DeleteMapping("/saved-jobs/{jobId}")
    public void unsaveJob(@PathVariable Long jobId, org.springframework.security.core.Authentication authentication) {
        UserEntity user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        jobRepository.findById(jobId).flatMap(job -> savedJobRepository.findByUserAndJob(user, job)).ifPresent(savedJobRepository::delete);
    }

    private JobDto toJobDto(JobEntity job) {
        return JobDto.builder().id(job.getId()).title(job.getTitle()).location(job.getLocation())
            .type(job.getType() == null ? null : job.getType().name()).experience(job.getExperience())
            .salaryMin(job.getSalaryMin()).salaryMax(job.getSalaryMax()).skills(job.getSkills())
            .description(job.getDescription()).responsibilities(job.getResponsibilities()).requirements(job.getRequirements())
            .posted(job.getPostedAt() == null ? null : job.getPostedAt().toLocalDate().toString()).saved(true).applied(false)
            .recruiter(job.getRecruiter() == null ? null : job.getRecruiter().getName()).build();
    }
}

package com.soareview.backend.controller;

import com.soareview.backend.dto.ApplicationDto;
import com.soareview.backend.entity.ApplicationEntity;
import com.soareview.backend.entity.ApplicationStatus;
import com.soareview.backend.entity.JobEntity;
import com.soareview.backend.entity.UserEntity;
import com.soareview.backend.repository.ApplicationRepository;
import com.soareview.backend.repository.JobRepository;
import com.soareview.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;

    @GetMapping("/applications")
    public List<ApplicationDto> listMyApplications(Authentication authentication) {
        UserEntity applicant = userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return applicationRepository.findByApplicantOrderByAppliedAtDesc(applicant).stream()
            .map(this::toDto)
            .toList();
    }

    @GetMapping("/applications/{id}")
    public ResponseEntity<ApplicationDto> getApplication(@PathVariable Long id, Authentication authentication) {
        UserEntity applicant = userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return applicationRepository.findById(id)
            .filter(app -> app.getApplicant().getId().equals(applicant.getId()))
            .map(this::toDto)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/jobs/{jobId}/apply")
    public ResponseEntity<ApplicationDto> applyToJob(@PathVariable Long jobId, Authentication authentication) {
        UserEntity applicant = userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        JobEntity job = jobRepository.findById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job not found"));

        if (applicationRepository.findByApplicantAndJob(applicant, job).stream().findAny().isPresent()) {
            throw new IllegalArgumentException("Application already exists");
        }

        ApplicationEntity application = ApplicationEntity.builder()
            .applicant(applicant)
            .job(job)
            .status(ApplicationStatus.APPLIED)
            .appliedAt(LocalDateTime.now())
            .coverLetter(null)
            .timeline(List.of(
                ApplicationEntity.ApplicationTimelineItem.builder()
                    .stage("Applied")
                    .date(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .completed(true)
                    .note("Application submitted successfully")
                    .build()))
            .build();

        ApplicationEntity saved = applicationRepository.save(application);
        return ResponseEntity.ok(toDto(saved));
    }

    @PatchMapping("/applications/{id}/status")
    public ResponseEntity<ApplicationDto> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> request, Authentication authentication) {
        UserEntity recruiter = userRepository.findByEmail(authentication.getName()).orElseThrow();
        ApplicationEntity application = applicationRepository.findById(id).orElseThrow();
        if (application.getJob().getRecruiter() == null || !application.getJob().getRecruiter().getId().equals(recruiter.getId())) {
            return ResponseEntity.status(403).build();
        }
        ApplicationStatus status = ApplicationStatus.valueOf(request.get("status").toUpperCase());
        application.setStatus(status);
        application.setTimeline(List.of(ApplicationEntity.ApplicationTimelineItem.builder()
            .stage(status.name().toLowerCase()).date(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
            .completed(true).note("Application status updated by recruiter").build()));
        return ResponseEntity.ok(toDto(applicationRepository.save(application)));
    }

    private ApplicationDto toDto(ApplicationEntity application) {
        return ApplicationDto.builder()
            .id(application.getId())
            .jobId(application.getJob().getId())
            .jobTitle(application.getJob().getTitle())
            .companyName(application.getJob().getCompany() == null ? "" : application.getJob().getCompany().getName())
            .status(formatStatus(application.getStatus()))
            .appliedDate(application.getAppliedAt() == null ? null : application.getAppliedAt().format(DateTimeFormatter.ISO_LOCAL_DATE))
            .nextInterview(null)
            .timeline(application.getTimeline() == null ? List.of() : application.getTimeline().stream()
                .map(item -> ApplicationDto.ApplicationTimelineItemDto.builder()
                    .stage(item.getStage())
                    .date(item.getDate())
                    .completed(item.isCompleted())
                    .note(item.getNote())
                    .build())
                .toList())
            .build();
    }

    private String formatStatus(ApplicationStatus status) {
        if (status == null) return "applied";
        return switch (status) {
            case APPLIED -> "applied";
            case UNDER_REVIEW -> "under_review";
            case SHORTLISTED -> "shortlisted";
            case INTERVIEW -> "interview";
            case FINAL_REVIEW -> "final_review";
            case SELECTED -> "selected";
            case REJECTED -> "rejected";
        };
    }
}

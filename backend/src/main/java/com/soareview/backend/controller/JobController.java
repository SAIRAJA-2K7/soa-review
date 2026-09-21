package com.soareview.backend.controller;

import com.soareview.backend.dto.JobCreateRequest;
import com.soareview.backend.dto.JobDto;
import com.soareview.backend.entity.CompanyEntity;
import com.soareview.backend.entity.JobEntity;
import com.soareview.backend.entity.JobType;
import com.soareview.backend.entity.UserEntity;
import com.soareview.backend.repository.CompanyRepository;
import com.soareview.backend.repository.JobRepository;
import com.soareview.backend.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class JobController {

    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    @GetMapping("/jobs")
    public List<JobDto> getJobs() {
        return jobRepository.findByPublishedTrueOrderByPostedAtDesc().stream()
            .map(this::toDto)
            .toList();
    }

    @GetMapping("/jobs/{id}")
    public ResponseEntity<JobDto> getJob(@PathVariable Long id) {
        return jobRepository.findById(id)
            .map(this::toDto)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/jobs")
    public ResponseEntity<JobDto> createJob(@Valid @RequestBody JobCreateRequest request, Authentication authentication) {
        String email = authentication.getName();
        UserEntity recruiter = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Recruiter not found"));

        CompanyEntity company = companyRepository.findByName(request.getCompanyName().trim())
            .orElseGet(() -> companyRepository.save(CompanyEntity.builder()
                .name(request.getCompanyName().trim())
                .logo(companyInitials(request.getCompanyName()))
                .location("Remote")
                .industry("Technology")
                .size("1-50")
                .build()));

        JobEntity job = JobEntity.builder()
            .title(request.getTitle().trim())
            .company(company)
            .location(request.getLocation().trim())
            .type(parseType(request.getType()))
            .experience(request.getExperience())
            .salaryMin(request.getSalaryMin())
            .salaryMax(request.getSalaryMax())
            .postedAt(LocalDateTime.now())
            .description(request.getDescription())
            .responsibilities(request.getResponsibilities() == null ? List.of() : request.getResponsibilities())
            .requirements(request.getRequirements() == null ? List.of() : request.getRequirements())
            .skills(request.getSkills() == null ? List.of() : request.getSkills())
            .published(true)
            .recruiter(recruiter)
            .createdAt(LocalDateTime.now())
            .build();

        return ResponseEntity.ok(toDto(jobRepository.save(job)));
    }

    @PutMapping("/jobs/{id}")
    public ResponseEntity<JobDto> updateJob(@PathVariable Long id, @Valid @RequestBody JobCreateRequest request, Authentication authentication) {
        UserEntity recruiter = userRepository.findByEmail(authentication.getName()).orElseThrow();
        JobEntity job = jobRepository.findById(id).orElseThrow();
        if (job.getRecruiter() == null || !job.getRecruiter().getId().equals(recruiter.getId())) {
            return ResponseEntity.status(403).build();
        }
        CompanyEntity company = companyRepository.findByName(request.getCompanyName().trim())
            .orElseGet(() -> companyRepository.save(CompanyEntity.builder()
                .name(request.getCompanyName().trim()).logo(companyInitials(request.getCompanyName()))
                .location("Remote").industry("Technology").size("1-50").build()));
        job.setTitle(request.getTitle().trim());
        job.setCompany(company);
        job.setLocation(request.getLocation().trim());
        job.setType(parseType(request.getType()));
        job.setExperience(request.getExperience());
        job.setSalaryMin(request.getSalaryMin());
        job.setSalaryMax(request.getSalaryMax());
        job.setDescription(request.getDescription());
        job.setResponsibilities(request.getResponsibilities() == null ? List.of() : request.getResponsibilities());
        job.setRequirements(request.getRequirements() == null ? List.of() : request.getRequirements());
        job.setSkills(request.getSkills() == null ? List.of() : request.getSkills());
        return ResponseEntity.ok(toDto(jobRepository.save(job)));
    }

    private JobDto toDto(JobEntity job) {
        return JobDto.builder()
            .id(job.getId())
            .title(job.getTitle())
            .company(job.getCompany() == null ? null : new com.soareview.backend.dto.CompanyDto(
                job.getCompany().getId(),
                job.getCompany().getName(),
                job.getCompany().getLogo(),
                job.getCompany().getIndustry(),
                job.getCompany().getSize(),
                job.getCompany().getLocation()))
            .location(job.getLocation())
            .type(formatType(job.getType()))
            .experience(job.getExperience())
            .salaryMin(job.getSalaryMin())
            .salaryMax(job.getSalaryMax())
            .posted(job.getPostedAt() == null ? null : job.getPostedAt().format(DateTimeFormatter.ISO_LOCAL_DATE))
            .skills(job.getSkills())
            .description(job.getDescription())
            .responsibilities(job.getResponsibilities())
            .requirements(job.getRequirements())
            .saved(false)
            .applied(false)
            .recruiter(job.getRecruiter() == null ? null : job.getRecruiter().getName())
            .build();
    }

    private JobType parseType(String type) {
        if (type == null) return JobType.FULL_TIME;
        return switch (type.trim().toUpperCase()) {
            case "PART_TIME" -> JobType.PART_TIME;
            case "CONTRACT" -> JobType.CONTRACT;
            case "REMOTE" -> JobType.REMOTE;
            default -> JobType.FULL_TIME;
        };
    }

    private String formatType(JobType type) {
        if (type == null) return "Full-time";
        return switch (type) {
            case FULL_TIME -> "Full-time";
            case PART_TIME -> "Part-time";
            case CONTRACT -> "Contract";
            case REMOTE -> "Remote";
        };
    }

    private String companyInitials(String companyName) {
        String[] parts = companyName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }
}

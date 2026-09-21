package com.soareview.backend.controller;

import com.soareview.backend.entity.ApplicationEntity;
import com.soareview.backend.entity.ApplicationStatus;
import com.soareview.backend.entity.InterviewEntity;
import com.soareview.backend.entity.InterviewStatus;
import com.soareview.backend.entity.JobEntity;
import com.soareview.backend.entity.UserEntity;
import com.soareview.backend.entity.CompanyEntity;
import com.soareview.backend.repository.ApplicationRepository;
import com.soareview.backend.repository.CompanyRepository;
import com.soareview.backend.repository.InterviewRepository;
import com.soareview.backend.repository.JobRepository;
import com.soareview.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;
    private final ApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;
    private final UserRepository userRepository;

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(Authentication authentication) {
        UserEntity recruiter = userRepository.findByEmail(authentication.getName()).orElseThrow();
        List<JobEntity> jobs = jobRepository.findByRecruiterOrderByPostedAtDesc(recruiter);
        List<ApplicationEntity> applications = jobs.stream()
            .flatMap(job -> applicationRepository.findByJobOrderByAppliedAtDesc(job).stream())
            .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activeJobs", jobs.stream().filter(JobEntity::isPublished).count());
        result.put("totalApplicants", applications.size());
        result.put("shortlisted", applications.stream().filter(application -> application.getStatus() == ApplicationStatus.SHORTLISTED).count());
        result.put("interviews", applications.stream().filter(application -> application.getStatus() == ApplicationStatus.INTERVIEW).count());
        result.put("selected", applications.stream().filter(application -> application.getStatus() == ApplicationStatus.SELECTED).count());
        result.put("jobs", jobs.stream().map(this::jobMap).toList());
        result.put("applications", applications.stream().map(this::applicationMap).toList());
        result.put("scheduledInterviews", interviewsFor(jobs).stream().map(this::interviewMap).toList());
        return result;
    }

    @GetMapping("/jobs")
    public List<Map<String, Object>> jobs(Authentication authentication) {
        UserEntity recruiter = userRepository.findByEmail(authentication.getName()).orElseThrow();
        return jobRepository.findByRecruiterOrderByPostedAtDesc(recruiter).stream().map(this::jobMap).toList();
    }

    @GetMapping("/applications")
    public List<Map<String, Object>> applications(Authentication authentication) {
        UserEntity recruiter = userRepository.findByEmail(authentication.getName()).orElseThrow();
        return jobRepository.findByRecruiterOrderByPostedAtDesc(recruiter).stream()
            .flatMap(job -> applicationRepository.findByJobOrderByAppliedAtDesc(job).stream())
            .map(this::applicationMap).toList();
    }

    @GetMapping("/candidates")
    public List<Map<String, Object>> candidates(Authentication authentication) {
        return applications(authentication).stream().map(application -> {
            Map<String, Object> candidate = new LinkedHashMap<>();
            candidate.put("id", application.get("candidateId"));
            candidate.put("name", application.get("candidateName"));
            candidate.put("email", application.get("candidateEmail"));
            candidate.put("status", application.get("status"));
            candidate.put("appliedJob", application.get("jobTitle"));
            candidate.put("appliedDate", application.get("appliedDate"));
            candidate.put("location", application.getOrDefault("candidateLocation", ""));
            candidate.put("experience", application.getOrDefault("candidateTitle", ""));
            candidate.put("skills", List.of());
            candidate.put("bio", application.getOrDefault("candidateBio", ""));
            candidate.put("rating", 0);
            return candidate;
        }).toList();
    }

    @GetMapping("/interviews")
    public List<Map<String, Object>> interviews(Authentication authentication) {
        UserEntity recruiter = userRepository.findByEmail(authentication.getName()).orElseThrow();
        List<JobEntity> jobs = jobRepository.findByRecruiterOrderByPostedAtDesc(recruiter);
        return interviewsFor(jobs).stream().map(this::interviewMap).toList();
    }

    @GetMapping("/company")
    public Map<String, Object> company(Authentication authentication) {
        return companyMap(companyFor(authentication));
    }

    @PatchMapping("/company")
    public Map<String, Object> updateCompany(@RequestBody Map<String, String> request, Authentication authentication) {
        CompanyEntity company = companyFor(authentication);
        if (request.containsKey("name")) company.setName(request.get("name").trim());
        if (request.containsKey("website")) company.setWebsite(request.get("website"));
        if (request.containsKey("size")) company.setSize(request.get("size"));
        if (request.containsKey("description")) company.setDescription(request.get("description"));
        return companyMap(companyRepository.save(company));
    }

    @PostMapping("/interviews")
    public ResponseEntity<Map<String, Object>> createInterview(@RequestBody Map<String, String> request, Authentication authentication) {
        UserEntity recruiter = userRepository.findByEmail(authentication.getName()).orElseThrow();
        UserEntity candidate = userRepository.findById(Long.valueOf(request.get("candidateId"))).orElseThrow();
        JobEntity job = jobRepository.findById(Long.valueOf(request.get("jobId"))).orElseThrow();
        InterviewEntity interview = interviewRepository.save(InterviewEntity.builder()
            .candidate(candidate).job(job)
            .scheduledAt(LocalDateTime.parse(request.get("scheduledAt")))
            .type(request.getOrDefault("type", "Technical"))
            .mode(request.getOrDefault("mode", "Video Call"))
            .interviewer(request.getOrDefault("interviewer", recruiter.getName()))
            .status(InterviewStatus.SCHEDULED).build());
        return ResponseEntity.ok(interviewMap(interview));
    }

    private List<InterviewEntity> interviewsFor(List<JobEntity> jobs) {
        return jobs.stream().flatMap(job -> interviewRepository.findByJobOrderByScheduledAtAsc(job).stream()).toList();
    }

    private CompanyEntity companyFor(Authentication authentication) {
        UserEntity recruiter = userRepository.findByEmail(authentication.getName()).orElseThrow();
        return jobRepository.findByRecruiterOrderByPostedAtDesc(recruiter).stream().findFirst()
            .map(JobEntity::getCompany).orElseThrow(() -> new IllegalArgumentException("Company not found"));
    }

    private Map<String, Object> companyMap(CompanyEntity company) {
        return Map.of("id", company.getId(), "name", company.getName(), "logo", company.getLogo() == null ? "" : company.getLogo(),
            "industry", company.getIndustry() == null ? "" : company.getIndustry(), "size", company.getSize() == null ? "" : company.getSize(),
            "location", company.getLocation() == null ? "" : company.getLocation(), "website", company.getWebsite() == null ? "" : company.getWebsite(),
            "description", company.getDescription() == null ? "" : company.getDescription());
    }

    private Map<String, Object> jobMap(JobEntity job) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", job.getId());
        result.put("title", job.getTitle());
        result.put("company", job.getCompany() == null ? "" : job.getCompany().getName());
        result.put("location", job.getLocation());
        result.put("type", job.getType() == null ? "FULL_TIME" : job.getType().name());
        result.put("experience", job.getExperience());
        result.put("salaryMin", job.getSalaryMin());
        result.put("salaryMax", job.getSalaryMax());
        result.put("posted", job.getPostedAt() == null ? null : job.getPostedAt().format(DateTimeFormatter.ISO_LOCAL_DATE));
        result.put("skills", job.getSkills() == null ? List.of() : job.getSkills());
        result.put("description", job.getDescription());
        result.put("published", job.isPublished());
        return result;
    }

    private Map<String, Object> applicationMap(ApplicationEntity application) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", application.getId());
        result.put("candidateId", application.getApplicant().getId());
        result.put("candidateName", application.getApplicant().getName());
        result.put("candidateEmail", application.getApplicant().getEmail());
        result.put("candidateTitle", application.getApplicant().getProfessionalTitle() == null ? "" : application.getApplicant().getProfessionalTitle());
        result.put("candidateLocation", application.getApplicant().getLocation() == null ? "" : application.getApplicant().getLocation());
        result.put("candidateBio", application.getApplicant().getBio() == null ? "" : application.getApplicant().getBio());
        result.put("jobId", application.getJob().getId());
        result.put("jobTitle", application.getJob().getTitle());
        result.put("companyName", application.getJob().getCompany().getName());
        result.put("status", application.getStatus().name().toLowerCase());
        result.put("appliedDate", application.getAppliedAt().format(DateTimeFormatter.ISO_LOCAL_DATE));
        result.put("timeline", application.getTimeline());
        return result;
    }

    private Map<String, Object> interviewMap(InterviewEntity interview) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", interview.getId());
        result.put("candidateId", interview.getCandidate().getId());
        result.put("candidate", interview.getCandidate().getName());
        result.put("jobId", interview.getJob().getId());
        result.put("job", interview.getJob().getTitle());
        result.put("date", interview.getScheduledAt().toLocalDate().toString());
        result.put("time", interview.getScheduledAt().toLocalTime().toString());
        result.put("type", interview.getType());
        result.put("mode", interview.getMode());
        result.put("status", interview.getStatus().name().toLowerCase());
        result.put("interviewer", interview.getInterviewer());
        return result;
    }
}

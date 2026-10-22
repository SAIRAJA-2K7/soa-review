package com.soareview.jobservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.client.RestTemplate;
import jakarta.annotation.PostConstruct;
import com.soareview.jobservice.persistence.JobDocumentStore;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class JobController {

    private static final Map<Long, Map<String, Object>> SEED_JOBS = new HashMap<>();

    private final RestTemplate restTemplate;
    private final String applicationServiceUrl;
    private final JobDocumentStore documentStore;
    private final String jobSyncSecret;

    public JobController(RestTemplate restTemplate,
                         @Value("${application-service.url:http://localhost:8084}") String applicationServiceUrl,
                         JobDocumentStore documentStore,
                         @Value("${job-sync.secret}") String jobSyncSecret) {
        this.restTemplate = restTemplate;
        this.applicationServiceUrl = applicationServiceUrl;
        this.documentStore = documentStore;
        this.jobSyncSecret = jobSyncSecret;
    }

    static {
        createSeedJob(1L, "Senior Product Engineer", "Nexus AI", "San Francisco, CA", "Full-time", "5-8 years", 160000, 220000,
                "Lead the architecture and development of core AI infrastructure.", List.of("TypeScript", "React", "Node.js", "AWS"));
        createSeedJob(2L, "Principal Systems Architect", "Orbital Systems", "Seattle, WA", "Full-time", "10+ years", 220000, 300000,
                "Architect the next generation of flight software systems.", List.of("C++", "RTOS", "Embedded Systems", "Linux"));
        createSeedJob(3L, "Clinical Data Scientist", "Meridian Health", "Boston, MA", "Full-time", "3-5 years", 110000, 150000,
                "Apply advanced data-science methods to clinical trial data.", List.of("Python", "R", "SQL", "Biostatistics"));
    }

    @PostConstruct
    void seedJobsWhenEmpty() {
        if (documentStore.findAll("jobs").isEmpty()) {
            SEED_JOBS.forEach((id, job) -> documentStore.save("jobs", String.valueOf(id), job));
        }
    }

    @GetMapping("/jobs")
    public List<Map<String, Object>> getJobs() {
        return documentStore.findAll("jobs").stream()
                .filter(job -> !Boolean.FALSE.equals(job.get("published")))
                .toList();
    }

    @GetMapping("/jobs/{id}")
    public Map<String, Object> getJob(@PathVariable Long id) {
        Map<String, Object> job = documentStore.find("jobs", String.valueOf(id));
        if (job == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found");
        }
        return job;
    }

    @PostMapping("/jobs")
    public Map<String, Object> createJob(@RequestBody Map<String, Object> payload,
                                         @RequestHeader(value = "X-User-Email", required = false) String recruiterEmail,
                                         @RequestHeader(value = "X-User-Role", required = false) String recruiterRole,
                                         @RequestHeader(value = "Authorization", required = false) String authorization) {
        requireRole(recruiterRole, "RECRUITER");
        requireIdentity(recruiterEmail);
        validateJobPayload(payload);
        Map<String, Object> job = buildJobFromPayload(0L, payload);
        job.put("recruiterEmail", recruiterEmail);
        Map<String, Object> saved = documentStore.create("jobs", job);
        try {
            synchronizeJob(saved, authorization);
        } catch (RuntimeException exception) {
            documentStore.delete("jobs", String.valueOf(saved.get("id")));
            throw exception;
        }
        return saved;
    }

    @PutMapping("/jobs/{id}")
    public Map<String, Object> updateJob(@PathVariable Long id, @RequestBody Map<String, Object> payload,
                                         @RequestHeader(value = "X-User-Email", required = false) String recruiterEmail,
                                         @RequestHeader(value = "X-User-Role", required = false) String recruiterRole,
                                         @RequestHeader(value = "Authorization", required = false) String authorization) {
        requireRole(recruiterRole, "RECRUITER");
        requireIdentity(recruiterEmail);
        validateJobPayload(payload);
        Map<String, Object> existing = documentStore.find("jobs", String.valueOf(id));
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found");
        }
        if (!recruiterEmail.equals(existing.get("recruiterEmail"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot update another recruiter's job");
        }

        Map<String, Object> updated = buildJobFromPayload(id, payload);
        updated.put("recruiterEmail", recruiterEmail);
        Map<String, Object> saved = documentStore.save("jobs", String.valueOf(id), updated);
        try {
            synchronizeJob(saved, authorization);
        } catch (RuntimeException exception) {
            documentStore.save("jobs", String.valueOf(id), existing);
            throw exception;
        }
        return saved;
    }

    @PostMapping("/jobs/{id}/apply")
    public Map<String, Object> applyToJob(@PathVariable Long id,
                                          @RequestBody(required = false) Map<String, Object> applicant,
                                          @RequestHeader(value = "X-User-Email", required = false) String candidateEmail,
                                          @RequestHeader(value = "X-User-Role", required = false) String candidateRole,
                                          @RequestHeader(value = "Authorization", required = false) String authorization) {
        requireRole(candidateRole, "USER");
        requireIdentity(candidateEmail);
        Map<String, Object> job = documentStore.find("jobs", String.valueOf(id));
        if (job == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Email", candidateEmail);
        headers.set("X-User-Role", candidateRole);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            headers.set(HttpHeaders.AUTHORIZATION, authorization);
        }
        Map<String, Object> application = new HashMap<>();
        application.put("candidateEmail", candidateEmail);
        application.put("jobId", id);
        application.put("jobTitle", job.get("title"));
        application.put("recruiterEmail", job.get("recruiterEmail"));
        Object company = job.get("company");
        Object companyName = company instanceof Map<?, ?> companyMap ? companyMap.get("name") : company;
        application.put("companyName", companyName == null ? "TalentSync" : String.valueOf(companyName));

        Map<String, Object> created = restTemplate.postForObject(
                applicationServiceUrl + "/api/applications", new HttpEntity<>(application, headers), Map.class);
        return created == null ? Map.of("jobId", id, "status", "APPLIED") : created;
    }

    @GetMapping("/user/saved-jobs")
    public List<Map<String, Object>> getSavedJobs(
            @RequestHeader(value = "X-User-Email", required = false) String candidateEmail,
            @RequestHeader(value = "X-User-Role", required = false) String candidateRole) {
        requireRole(candidateRole, "USER");
        requireIdentity(candidateEmail);
        List<Map<String, Object>> savedJobs = documentStore.findAll("saved-jobs").stream()
                .filter(saved -> candidateEmail != null && candidateEmail.equals(saved.get("candidateEmail")))
                .toList();
        List<String> jobKeys = savedJobs.stream()
                .map(saved -> String.valueOf(saved.get("jobId")))
                .distinct()
                .toList();
        Map<String, Map<String, Object>> jobsById = new HashMap<>();
        documentStore.findAllByKeys("jobs", jobKeys).forEach(job ->
                jobsById.put(String.valueOf(job.get("id")), job));
        return savedJobs.stream()
                .map(saved -> jobsById.get(String.valueOf(saved.get("jobId"))))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @PostMapping("/user/saved-jobs/{id}")
    public Map<String, String> saveJob(@PathVariable Long id,
                                       @RequestHeader(value = "X-User-Email", required = false) String candidateEmail,
                                       @RequestHeader(value = "X-User-Role", required = false) String candidateRole) {
        requireRole(candidateRole, "USER");
        requireIdentity(candidateEmail);
        if (documentStore.find("jobs", String.valueOf(id)) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found");
        }
        Map<String, Object> saved = Map.of("candidateEmail", candidateEmail, "jobId", id);
        documentStore.save("saved-jobs", candidateEmail + ":" + id, saved);
        return Map.of("status", "saved");
    }

    @DeleteMapping("/user/saved-jobs/{id}")
    public Map<String, String> removeSavedJob(@PathVariable Long id,
                                              @RequestHeader(value = "X-User-Email", required = false) String candidateEmail,
                                              @RequestHeader(value = "X-User-Role", required = false) String candidateRole) {
        requireRole(candidateRole, "USER");
        requireIdentity(candidateEmail);
        if (documentStore.find("jobs", String.valueOf(id)) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found");
        }
        documentStore.delete("saved-jobs", candidateEmail + ":" + id);
        return Map.of("status", "removed");
    }

    private static void createSeedJob(Long id, String title, String companyName, String location, String type,
                                     String experience, Integer salaryMin, Integer salaryMax,
                                     String description, List<String> skills) {
        Map<String, Object> company = new HashMap<>();
        company.put("id", id);
        company.put("name", companyName);
        company.put("logo", companyName.substring(0, 2).toUpperCase());

        Map<String, Object> job = new HashMap<>();
        job.put("id", id);
        job.put("title", title);
        job.put("company", company);
        job.put("location", location);
        job.put("type", type);
        job.put("experience", experience);
        job.put("salaryMin", salaryMin);
        job.put("salaryMax", salaryMax);
        job.put("description", description);
        job.put("skills", skills);
        job.put("posted", Instant.now().toString());
        job.put("published", true);
        job.put("recruiterEmail", "recruiter@demo.com");
        SEED_JOBS.put(id, job);
    }

    private Map<String, Object> buildJobFromPayload(Long id, Map<String, Object> payload) {
        Map<String, Object> company = new HashMap<>();
        company.put("id", id);
        company.put("name", String.valueOf(payload.getOrDefault("companyName", "TalentSync")));
        company.put("logo", String.valueOf(payload.getOrDefault("companyName", "TalentSync")).substring(0, 1).toUpperCase());

        Map<String, Object> job = new HashMap<>();
        job.put("id", id);
        job.put("title", payload.getOrDefault("title", "Untitled Position"));
        job.put("company", company);
        job.put("location", payload.getOrDefault("location", "Remote"));
        job.put("type", payload.getOrDefault("type", "Full-time"));
        job.put("experience", payload.getOrDefault("experience", "2+ years"));
        job.put("salaryMin", payload.getOrDefault("salaryMin", 0));
        job.put("salaryMax", payload.getOrDefault("salaryMax", 0));
        job.put("description", payload.getOrDefault("description", ""));
        job.put("skills", payload.getOrDefault("skills", List.of("Java", "Spring")));
        job.put("posted", Instant.now().toString());
        job.put("published", true);
        return job;
    }

    @GetMapping("/admin/jobs")
    public List<Map<String, Object>> getRecruiterJobs(
            @RequestHeader(value = "X-User-Email", required = false) String recruiterEmail,
            @RequestHeader(value = "X-User-Role", required = false) String recruiterRole) {
        requireRole(recruiterRole, "RECRUITER");
        return documentStore.findAll("jobs").stream()
                .filter(job -> recruiterEmail != null && recruiterEmail.equals(job.get("recruiterEmail")))
                .toList();
    }

    private void requireRole(String actualRole, String requiredRole) {
        if (!requiredRole.equals(actualRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient role");
        }
    }

    private void requireIdentity(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user identity is required");
        }
    }

    private void validateJobPayload(Map<String, Object> payload) {
        if (payload == null || hasBlank(payload, "title", "companyName", "location", "description")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Title, company name, location, and description are required");
        }

        BigDecimal minimum = salary(payload.get("salaryMin"), "Minimum salary");
        BigDecimal maximum = salary(payload.get("salaryMax"), "Maximum salary");
        if (minimum != null && minimum.signum() < 0 || maximum != null && maximum.signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Salary cannot be negative");
        }
        if (minimum != null && maximum != null && minimum.compareTo(maximum) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Minimum salary cannot exceed maximum salary");
        }
    }

    private boolean hasBlank(Map<String, Object> payload, String... fields) {
        for (String field : fields) {
            Object value = payload.get(field);
            if (value == null || String.valueOf(value).isBlank()) return true;
        }
        return false;
    }

    private BigDecimal salary(Object value, String fieldName) {
        if (value == null || String.valueOf(value).isBlank()) return null;
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be numeric");
        }
    }

    private void synchronizeJob(Map<String, Object> job, String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bearer token is required");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, authorization);
        headers.set("X-Service-Token", jobSyncSecret);
        try {
            restTemplate.exchange(applicationServiceUrl + "/api/internal/jobs/" + job.get("id"),
                    org.springframework.http.HttpMethod.PUT, new HttpEntity<>(job, headers), Map.class);
        } catch (org.springframework.web.client.RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Job was saved, but recruiter data synchronization is unavailable", exception);
        }
    }
}

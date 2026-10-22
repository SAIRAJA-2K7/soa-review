package com.soareview.applicationservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import jakarta.annotation.PostConstruct;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import com.soareview.applicationservice.persistence.ApplicationDocumentStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;

@RestController
@RequestMapping("/api")
public class ApplicationController {

    private static final List<Map<String, Object>> APPLICATIONS = new ArrayList<>();
    private static final List<Map<String, Object>> JOBS = new ArrayList<>();
    private static final List<Map<String, Object>> INTERVIEWS = new ArrayList<>();
    private static final Map<String, Object> COMPANY = new LinkedHashMap<>();

    private final ApplicationDocumentStore documentStore;
    private final RestTemplate restTemplate;
    private final String authServiceUrl;
    private final String jobSyncSecret;

    public ApplicationController(ApplicationDocumentStore documentStore,
                                 RestTemplate restTemplate,
                                 @Value("${auth-service.url:http://AUTH-SERVICE}") String authServiceUrl,
                                 @Value("${job-sync.secret}") String jobSyncSecret) {
        this.documentStore = documentStore;
        this.restTemplate = restTemplate;
        this.authServiceUrl = authServiceUrl;
        this.jobSyncSecret = jobSyncSecret;
    }

    private static Map<String, Object> mapOf(Object... entries) {
        if (entries.length % 2 != 0) {
            throw new IllegalArgumentException("Expected even number of key/value arguments");
        }
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            map.put(String.valueOf(entries[i]), entries[i + 1]);
        }
        return map;
    }

    static {
        JOBS.add(mapOf(
                "id", 1,
                "title", "Senior Product Engineer",
                "company", "Nexus AI",
                "location", "San Francisco, CA",
                "type", "Full-time",
                "experience", "5-8 years",
                "salaryMin", 160000,
                "salaryMax", 220000,
                "posted", "2026-09-01",
                "skills", List.of("TypeScript", "React", "Node.js", "AWS"),
                "description", "Lead the architecture and development of core AI infrastructure.",
                "published", true
        ));
        JOBS.add(mapOf(
                "id", 2,
                "title", "Principal Systems Architect",
                "company", "Orbital Systems",
                "location", "Seattle, WA",
                "type", "Full-time",
                "experience", "10+ years",
                "salaryMin", 220000,
                "salaryMax", 300000,
                "posted", "2026-09-05",
                "skills", List.of("C++", "RTOS", "Embedded Systems", "Linux"),
                "description", "Architect the next generation of flight software systems.",
                "published", true
        ));
        JOBS.add(mapOf(
                "id", 3,
                "title", "Clinical Data Scientist",
                "company", "Meridian Health",
                "location", "Boston, MA",
                "type", "Full-time",
                "experience", "3-5 years",
                "salaryMin", 110000,
                "salaryMax", 150000,
                "posted", "2026-09-10",
                "skills", List.of("Python", "R", "SQL", "Biostatistics"),
                "description", "Apply advanced data-science methods to clinical trial data.",
                "published", true
        ));

        APPLICATIONS.add(mapOf(
                "id", 1,
                "candidateId", 101,
                "candidateName", "Alex Morgan",
                "candidateEmail", "alex@demo.com",
                "candidateTitle", "AI Platform Engineer",
                "candidateLocation", "San Francisco, CA",
                "candidateBio", "Builds resilient applied-AI systems for enterprise workloads.",
                "jobId", 1,
                "jobTitle", "Senior Product Engineer",
                "companyName", "Nexus AI",
                "status", "interview",
                "appliedDate", "2026-09-01",
                "timeline", List.of(mapOf("stage", "Applied", "date", "2026-09-01", "completed", true, "note", "Application submitted successfully"))
        ));
        APPLICATIONS.add(mapOf(
                "id", 2,
                "candidateId", 102,
                "candidateName", "Priya Sharma",
                "candidateEmail", "priya@example.com",
                "candidateTitle", "Systems Architect",
                "candidateLocation", "Seattle, WA",
                "candidateBio", "Leads architecture for cloud-native distributed platforms.",
                "jobId", 2,
                "jobTitle", "Principal Systems Architect",
                "companyName", "Orbital Systems",
                "status", "shortlisted",
                "appliedDate", "2026-09-03",
                "timeline", List.of(mapOf("stage", "Applied", "date", "2026-09-03", "completed", true, "note", "Application submitted successfully"))
        ));
        APPLICATIONS.add(mapOf(
                "id", 3,
                "candidateId", 103,
                "candidateName", "Daniel Wu",
                "candidateEmail", "daniel@example.com",
                "candidateTitle", "Clinical Data Scientist",
                "candidateLocation", "Boston, MA",
                "candidateBio", "Works across experimentation and lifecycle analytics in healthcare.",
                "jobId", 3,
                "jobTitle", "Clinical Data Scientist",
                "companyName", "Meridian Health",
                "status", "under_review",
                "appliedDate", "2026-09-05",
                "timeline", List.of(mapOf("stage", "Applied", "date", "2026-09-05", "completed", true, "note", "Application submitted successfully"))
        ));

        INTERVIEWS.add(mapOf(
                "id", 1001,
                "candidateId", 101,
                "candidate", "Alex Morgan",
                "jobId", 1,
                "job", "Senior Product Engineer",
                "date", "2026-09-25",
                "time", "10:30:00",
                "type", "Technical",
                "mode", "Video Call",
                "status", "scheduled",
                "interviewer", "Sarah Chen"
        ));
        INTERVIEWS.add(mapOf(
                "id", 1002,
                "candidateId", 102,
                "candidate", "Priya Sharma",
                "jobId", 2,
                "job", "Principal Systems Architect",
                "date", "2026-09-27",
                "time", "14:00:00",
                "type", "Final",
                "mode", "Zoom",
                "status", "scheduled",
                "interviewer", "Priya Nair"
        ));

        COMPANY.put("id", 1);
        COMPANY.put("name", "TalentSync");
        COMPANY.put("logo", "TS");
        COMPANY.put("industry", "Technology");
        COMPANY.put("size", "11-50");
        COMPANY.put("location", "Remote");
        COMPANY.put("website", "https://talentsync.example.com");
        COMPANY.put("description", "We build talent infrastructure for modern hiring teams.");
    }

    @PostConstruct
    void seedPersistentCollections() {
        seedCollection("jobs", JOBS);
        if (documentStore.findAll("applications").isEmpty()) {
            APPLICATIONS.forEach(application -> {
                application.put("recruiterEmail", "recruiter@demo.com");
                documentStore.save("applications", String.valueOf(application.get("id")), application);
            });
        }
        if (documentStore.findAll("interviews").isEmpty()) {
            List<Map<String, Object>> applications = documentStore.findAll("applications");
            INTERVIEWS.forEach(interview -> {
                String candidateId = String.valueOf(interview.get("candidateId"));
                applications.stream()
                        .filter(application -> candidateId.equals(String.valueOf(application.get("candidateId"))))
                        .findFirst()
                        .ifPresent(application -> interview.put("candidateEmail", application.get("candidateEmail")));
                interview.putIfAbsent("recruiterEmail", "recruiter@demo.com");
                documentStore.save("interviews", String.valueOf(interview.get("id")), interview);
            });
        }
        if (documentStore.find("company", "recruiter@demo.com") == null) {
            documentStore.save("company", "recruiter@demo.com", COMPANY);
        }
    }

    private void seedCollection(String name, List<Map<String, Object>> seeds) {
        if (documentStore.findAll(name).isEmpty()) {
            seeds.forEach(record -> {
                record.putIfAbsent("recruiterEmail", "recruiter@demo.com");
                documentStore.save(name, String.valueOf(record.get("id")), record);
            });
        }
    }

    @PutMapping("/internal/jobs/{id}")
    public Map<String, Object> syncRecruiterJob(@PathVariable Long id, @RequestBody Map<String, Object> job,
                                                @RequestHeader(value = "X-User-Email", required = false) String recruiterEmail,
                                                @RequestHeader(value = "X-Service-Token", required = false) String serviceToken) {
        requireIdentity(recruiterEmail);
        if (!StringUtils.hasText(serviceToken) || !java.security.MessageDigest.isEqual(
                jobSyncSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                serviceToken.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Service authentication required");
        }
        Map<String, Object> persisted = new LinkedHashMap<>(job);
        persisted.put("id", id);
        persisted.put("recruiterEmail", recruiterEmail);
        return documentStore.save("jobs", String.valueOf(id), persisted);
    }

    @GetMapping("/applications")
    public List<Map<String, Object>> getApplications(
            @RequestHeader(value = "X-User-Email", required = false) String candidateEmail) {
        requireIdentity(candidateEmail);
        return documentStore.findAll("applications").stream()
                .filter(application -> candidateEmail.equalsIgnoreCase(String.valueOf(application.get("candidateEmail"))))
                .toList();
    }

    @GetMapping("/applications/{id}")
    public Map<String, Object> getApplication(@PathVariable Long id,
                                               @RequestHeader(value = "X-User-Email", required = false) String candidateEmail) {
        requireIdentity(candidateEmail);
        Map<String, Object> application = documentStore.find("applications", String.valueOf(id));
        if (application == null || !candidateEmail.equalsIgnoreCase(String.valueOf(application.get("candidateEmail")))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found");
        }
        return application;
    }

    @PostMapping("/applications")
    public Map<String, Object> createApplication(@RequestBody Map<String, Object> request,
                                                 @RequestHeader(value = "X-User-Email", required = false) String authenticatedEmail,
                                                 @RequestHeader(value = "Authorization", required = false) String authorization) {
        Object requestedJobId = request.get("jobId");
        String jobId = requestedJobId == null ? "" : String.valueOf(requestedJobId);

        requireIdentity(authenticatedEmail);
        if (jobId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Job id is required");
        }

        Map<String, Object> candidateProfile = loadCandidateProfile(authorization);
        String candidateEmail = String.valueOf(candidateProfile.get("email")).trim().toLowerCase();
        if (!candidateEmail.equalsIgnoreCase(authenticatedEmail)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Candidate identity does not match the JWT");
        }

        Map<String, Object> job = documentStore.find("jobs", jobId);
        if (job == null || Boolean.FALSE.equals(job.get("published"))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Published job not found");
        }

        String appliedDate = LocalDate.now().toString();
        Map<String, Object> application = new LinkedHashMap<>();
        application.put("candidateId", candidateProfile.get("id"));
        application.put("candidateName", candidateProfile.getOrDefault("name", candidateEmail));
        application.put("candidateEmail", candidateEmail);
        application.put("candidateTitle", candidateProfile.getOrDefault("professionalTitle", ""));
        application.put("candidateLocation", candidateProfile.getOrDefault("location", ""));
        application.put("candidateBio", candidateProfile.getOrDefault("bio", ""));
        application.put("jobId", request.get("jobId"));
        application.put("jobTitle", job.getOrDefault("title", "Open Role"));
        Object company = job.get("company");
        Object companyName = company instanceof Map<?, ?> companyMap ? companyMap.get("name") : company;
        application.put("companyName", companyName == null
            ? job.getOrDefault("companyName", "TalentSync") : String.valueOf(companyName));
        application.put("recruiterEmail", job.getOrDefault("recruiterEmail", ""));
        application.put("status", "under_review");
        application.put("appliedDate", appliedDate);
        application.put("timeline", List.of(mapOf(
                "stage", "Applied",
                "date", appliedDate,
                "completed", true,
                "note", "Application submitted successfully")));
        try {
            return documentStore.createUnique("applications", "application-reservations",
                    candidateEmail + ":" + jobId, application);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already applied to this job", exception);
        }
    }

    private Map<String, Object> loadCandidateProfile(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bearer token is required");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization);
        try {
            Map<String, Object> profile = restTemplate.exchange(authServiceUrl + "/api/user/profile",
                    HttpMethod.GET, new HttpEntity<>(headers), Map.class).getBody();
            if (profile == null || profile.get("email") == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Auth service returned no candidate profile");
            }
            return profile;
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Candidate profile service is unavailable", exception);
        }
    }

    @GetMapping("/user/interviews")
        public List<Map<String, Object>> getUserInterviews(
            @RequestHeader(value = "X-User-Email", required = false) String candidateEmail) {
        requireIdentity(candidateEmail);
        return documentStore.findAll("interviews").stream()
            .filter(interview -> candidateEmail.equalsIgnoreCase(String.valueOf(interview.get("candidateEmail"))))
            .map(interview -> mapOf(
                "id", interview.get("id"),
                "type", interview.get("type"),
                "job", interview.get("job"),
                "date", interview.get("date"),
                "time", interview.get("time"),
                "mode", interview.get("mode"),
                "interviewer", interview.get("interviewer")))
            .toList();
    }

    @PatchMapping("/applications/{id}/status")
    public Map<String, Object> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> request,
                                            @RequestHeader(value = "X-User-Email", required = false) String recruiterEmail) {
        requireIdentity(recruiterEmail);
        String status = request.getOrDefault("status", "under_review").trim().toLowerCase();
        if (!List.of("under_review", "shortlisted", "interview", "selected", "rejected").contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported application status");
        }
        Map<String, Object> application = documentStore.find("applications", String.valueOf(id));
        if (application == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found");
        }
        if (!recruiterEmail.equalsIgnoreCase(String.valueOf(application.get("recruiterEmail")))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot update another recruiter's application");
        }
        application.put("status", status);
        List<Map<String, Object>> timeline = new ArrayList<>();
        Object currentTimeline = application.get("timeline");
        if (currentTimeline instanceof List<?> items) {
            items.stream().filter(Map.class::isInstance).map(item -> (Map<String, Object>) item).forEach(timeline::add);
        }
        timeline.add(mapOf("stage", status.replace('_', ' '), "date", LocalDate.now().toString(),
                "completed", true, "note", "Application status updated by recruiter"));
        application.put("timeline", timeline);
        return documentStore.save("applications", String.valueOf(id), application);
    }

    @GetMapping("/admin/dashboard")
    public Map<String, Object> recruiterDashboard(
            @RequestHeader(value = "X-User-Email", required = false) String recruiterEmail) {
        requireIdentity(recruiterEmail);
        List<Map<String, Object>> applications = applicationsForRecruiter(recruiterEmail);
        List<Map<String, Object>> interviews = interviewsForRecruiter(recruiterEmail);
        List<Map<String, Object>> jobs = jobsForRecruiter(recruiterEmail);
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("activeJobs", jobs.stream().filter(job -> !Boolean.FALSE.equals(job.get("published"))).count());
        dashboard.put("totalApplicants", applications.size());
        dashboard.put("shortlisted", (int) applications.stream().filter(app -> "shortlisted".equalsIgnoreCase(String.valueOf(app.get("status")))).count());
        dashboard.put("interviews", interviews.size());
        dashboard.put("jobs", jobs);
        dashboard.put("applications", applications);
        dashboard.put("scheduledInterviews", interviews);
        return dashboard;
    }

    @GetMapping("/admin/jobs")
    public List<Map<String, Object>> recruiterJobs(
            @RequestHeader(value = "X-User-Email", required = false) String recruiterEmail) {
        requireIdentity(recruiterEmail);
        return jobsForRecruiter(recruiterEmail);
    }

    @GetMapping("/admin/applications")
    public List<Map<String, Object>> recruiterApplications(
            @RequestHeader(value = "X-User-Email", required = false) String recruiterEmail) {
        requireIdentity(recruiterEmail);
        return applicationsForRecruiter(recruiterEmail);
    }

    @GetMapping("/admin/candidates")
    public List<Map<String, Object>> recruiterCandidates(
            @RequestHeader(value = "X-User-Email", required = false) String recruiterEmail) {
        requireIdentity(recruiterEmail);
        return applicationsForRecruiter(recruiterEmail).stream().map(application -> {
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

    @GetMapping("/admin/interviews")
    public List<Map<String, Object>> recruiterInterviews(
            @RequestHeader(value = "X-User-Email", required = false) String recruiterEmail) {
        requireIdentity(recruiterEmail);
        return interviewsForRecruiter(recruiterEmail);
    }

    @PostMapping("/admin/interviews")
    public Map<String, Object> createInterview(@RequestBody Map<String, Object> payload,
                                                @RequestHeader(value = "X-User-Email", required = false) String recruiterEmail) {
        requireIdentity(recruiterEmail);
        Map<String, Object> interview = new LinkedHashMap<>();
        Long candidateId = Long.valueOf(String.valueOf(payload.getOrDefault("candidateId", 101)));
        Long jobId = Long.valueOf(String.valueOf(payload.getOrDefault("jobId", 1)));
        List<Map<String, Object>> applications = applicationsForRecruiter(recruiterEmail);
        List<Map<String, Object>> jobs = jobsForRecruiter(recruiterEmail);
        boolean candidateApplied = applications.stream().anyMatch(application ->
            String.valueOf(application.get("candidateId")).equals(String.valueOf(candidateId)) &&
            String.valueOf(application.get("jobId")).equals(String.valueOf(jobId)));
        if (!candidateApplied) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Candidate must have an application for this job");
        }
        String candidateName = applications.stream()
                .filter(app -> String.valueOf(app.get("candidateId")).equals(String.valueOf(candidateId)))
                .map(app -> String.valueOf(app.get("candidateName")))
                .findFirst()
                .orElse("Candidate");
        String jobTitle = jobs.stream()
                .filter(job -> String.valueOf(job.get("id")).equals(String.valueOf(jobId)))
                .map(job -> String.valueOf(job.get("title")))
                .findFirst()
                .orElse("Open Role");

        interview.put("candidateId", candidateId);
        interview.put("candidate", candidateName);
        interview.put("jobId", jobId);
        interview.put("job", jobTitle);
        interview.put("date", String.valueOf(payload.getOrDefault("scheduledAt", "2026-09-30T10:00")));
        interview.put("time", String.valueOf(payload.getOrDefault("scheduledAt", "2026-09-30T10:00")).contains("T")
                ? String.valueOf(payload.getOrDefault("scheduledAt", "2026-09-30T10:00")).split("T")[1]
                : "10:00:00");
        interview.put("type", payload.getOrDefault("type", "Technical"));
        interview.put("mode", payload.getOrDefault("mode", "Video Call"));
        interview.put("status", "scheduled");
        interview.put("interviewer", "Hiring Team");
        interview.put("candidateEmail", applications.stream()
            .filter(application -> String.valueOf(application.get("candidateId")).equals(String.valueOf(candidateId)))
            .map(application -> String.valueOf(application.get("candidateEmail"))).findFirst().orElse(""));
        interview.put("recruiterEmail", recruiterEmail);
        return documentStore.create("interviews", interview);
    }

    @GetMapping("/admin/company")
    public Map<String, Object> recruiterCompany(
            @RequestHeader(value = "X-User-Email", required = false) String recruiterEmail) {
        requireIdentity(recruiterEmail);
        return companyFor(recruiterEmail);
    }

    @PatchMapping("/admin/company")
    public Map<String, Object> updateCompany(@RequestBody Map<String, String> request,
                                             @RequestHeader(value = "X-User-Email", required = false) String recruiterEmail) {
        requireIdentity(recruiterEmail);
        Map<String, Object> company = companyFor(recruiterEmail);
        if (request.containsKey("name")) company.put("name", request.get("name"));
        if (request.containsKey("website")) company.put("website", request.get("website"));
        if (request.containsKey("size")) company.put("size", request.get("size"));
        if (request.containsKey("description")) company.put("description", request.get("description"));
        return documentStore.save("company", recruiterEmail, company);
    }

    private void requireIdentity(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user identity is required");
        }
    }

    private List<Map<String, Object>> applicationsForRecruiter(String recruiterEmail) {
        return documentStore.findAll("applications").stream()
                .filter(application -> recruiterEmail.equalsIgnoreCase(String.valueOf(application.get("recruiterEmail"))))
                .toList();
    }

    private List<Map<String, Object>> interviewsForRecruiter(String recruiterEmail) {
        return documentStore.findAll("interviews").stream()
                .filter(interview -> recruiterEmail.equalsIgnoreCase(String.valueOf(interview.get("recruiterEmail"))))
                .toList();
    }

    private List<Map<String, Object>> jobsForRecruiter(String recruiterEmail) {
        return documentStore.findAll("jobs").stream()
                .filter(job -> recruiterEmail.equalsIgnoreCase(String.valueOf(job.get("recruiterEmail"))))
                .toList();
    }

    private Map<String, Object> companyFor(String recruiterEmail) {
        Map<String, Object> company = documentStore.find("company", recruiterEmail);
        if (company != null) return company;
        Map<String, Object> defaults = new LinkedHashMap<>(COMPANY);
        return documentStore.save("company", recruiterEmail, defaults);
    }
}

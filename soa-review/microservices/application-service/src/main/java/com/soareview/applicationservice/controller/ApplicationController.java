package com.soareview.applicationservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api")
public class ApplicationController {

    private static final List<Map<String, Object>> APPLICATIONS = new ArrayList<>();
    private static final AtomicLong APPLICATION_ID_GENERATOR = new AtomicLong(4L);
    private static final List<Map<String, Object>> JOBS = new ArrayList<>();
    private static final List<Map<String, Object>> INTERVIEWS = new ArrayList<>();
    private static final Map<String, Object> COMPANY = new LinkedHashMap<>();

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

    @GetMapping("/applications")
    public List<Map<String, Object>> getApplications() {
        return APPLICATIONS;
    }

    @GetMapping("/applications/{id}")
    public Map<String, Object> getApplication(@PathVariable Long id) {
        return APPLICATIONS.stream()
                .filter(app -> String.valueOf(app.get("id")).equals(String.valueOf(id)))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
    }

    @PostMapping("/applications")
    public synchronized Map<String, Object> createApplication(@RequestBody Map<String, Object> request) {
        String candidateEmail = String.valueOf(request.getOrDefault("candidateEmail", "")).trim().toLowerCase();
        String jobId = String.valueOf(request.getOrDefault("jobId", ""));

        if (candidateEmail.isBlank() || jobId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Candidate email and job id are required");
        }

        boolean alreadyApplied = APPLICATIONS.stream().anyMatch(application ->
                candidateEmail.equalsIgnoreCase(String.valueOf(application.get("candidateEmail"))) &&
                jobId.equals(String.valueOf(application.get("jobId"))));
        if (alreadyApplied) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already applied to this job");
        }

        String appliedDate = LocalDate.now().toString();
        Map<String, Object> application = new LinkedHashMap<>();
        application.put("id", APPLICATION_ID_GENERATOR.getAndIncrement());
        application.put("candidateId", request.getOrDefault("candidateId", candidateEmail));
        application.put("candidateName", request.getOrDefault("candidateName", candidateEmail));
        application.put("candidateEmail", candidateEmail);
        application.put("candidateTitle", request.getOrDefault("candidateTitle", ""));
        application.put("candidateLocation", request.getOrDefault("candidateLocation", ""));
        application.put("candidateBio", request.getOrDefault("candidateBio", ""));
        application.put("jobId", request.get("jobId"));
        application.put("jobTitle", request.getOrDefault("jobTitle", "Open Role"));
        application.put("companyName", request.getOrDefault("companyName", "TalentSync"));
        application.put("status", "under_review");
        application.put("appliedDate", appliedDate);
        application.put("timeline", List.of(mapOf(
                "stage", "Applied",
                "date", appliedDate,
                "completed", true,
                "note", "Application submitted successfully")));
        APPLICATIONS.add(application);
        return application;
    }

    @GetMapping("/user/interviews")
    public List<Map<String, Object>> getUserInterviews() {
        List<Map<String, Object>> interviews = new ArrayList<>();
        interviews.add(mapOf(
                "id", 1001,
                "type", "Technical",
                "job", "Senior Product Engineer",
                "date", "2026-09-25",
                "time", "10:30 AM",
                "mode", "Video",
                "interviewer", "Sarah Chen"
        ));
        interviews.add(mapOf(
                "id", 1002,
                "type", "Final",
                "job", "Clinical Data Scientist",
                "date", "2026-09-27",
                "time", "2:00 PM",
                "mode", "Zoom",
                "interviewer", "Priya Nair"
        ));
        return interviews;
    }

    @PatchMapping("/applications/{id}/status")
    public Map<String, Object> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String status = request.getOrDefault("status", "UNDER_REVIEW");
        Map<String, Object> application = getApplication(id);
        application.put("status", status.toLowerCase());
        return application;
    }

    @GetMapping("/admin/dashboard")
    public Map<String, Object> recruiterDashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("activeJobs", JOBS.size());
        dashboard.put("totalApplicants", APPLICATIONS.size());
        dashboard.put("shortlisted", (int) APPLICATIONS.stream().filter(app -> "shortlisted".equalsIgnoreCase(String.valueOf(app.get("status")))).count());
        dashboard.put("interviews", INTERVIEWS.size());
        dashboard.put("jobs", JOBS);
        dashboard.put("applications", APPLICATIONS);
        dashboard.put("scheduledInterviews", INTERVIEWS);
        return dashboard;
    }

    @GetMapping("/admin/jobs")
    public List<Map<String, Object>> recruiterJobs() {
        return JOBS;
    }

    @GetMapping("/admin/applications")
    public List<Map<String, Object>> recruiterApplications() {
        return APPLICATIONS;
    }

    @GetMapping("/admin/candidates")
    public List<Map<String, Object>> recruiterCandidates() {
        return APPLICATIONS.stream().map(application -> {
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
    public List<Map<String, Object>> recruiterInterviews() {
        return INTERVIEWS;
    }

    @PostMapping("/admin/interviews")
    public Map<String, Object> createInterview(@RequestBody Map<String, Object> payload) {
        Map<String, Object> interview = new LinkedHashMap<>();
        Long candidateId = Long.valueOf(String.valueOf(payload.getOrDefault("candidateId", 101)));
        Long jobId = Long.valueOf(String.valueOf(payload.getOrDefault("jobId", 1)));
        String candidateName = APPLICATIONS.stream()
                .filter(app -> String.valueOf(app.get("candidateId")).equals(String.valueOf(candidateId)))
                .map(app -> String.valueOf(app.get("candidateName")))
                .findFirst()
                .orElse("Candidate");
        String jobTitle = JOBS.stream()
                .filter(job -> String.valueOf(job.get("id")).equals(String.valueOf(jobId)))
                .map(job -> String.valueOf(job.get("title")))
                .findFirst()
                .orElse("Open Role");

        long nextId = INTERVIEWS.stream().mapToLong(item -> Long.parseLong(String.valueOf(item.get("id")))).max().orElse(1000L) + 1L;
        interview.put("id", nextId);
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
        INTERVIEWS.add(interview);
        return interview;
    }

    @GetMapping("/admin/company")
    public Map<String, Object> recruiterCompany() {
        return COMPANY;
    }

    @PatchMapping("/admin/company")
    public Map<String, Object> updateCompany(@RequestBody Map<String, String> request) {
        if (request.containsKey("name")) COMPANY.put("name", request.get("name"));
        if (request.containsKey("website")) COMPANY.put("website", request.get("website"));
        if (request.containsKey("size")) COMPANY.put("size", request.get("size"));
        if (request.containsKey("description")) COMPANY.put("description", request.get("description"));
        return COMPANY;
    }
}

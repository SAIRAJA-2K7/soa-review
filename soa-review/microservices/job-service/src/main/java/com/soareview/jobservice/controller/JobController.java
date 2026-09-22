package com.soareview.jobservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api")
public class JobController {

    private static final Map<Long, Map<String, Object>> JOBS = new ConcurrentHashMap<>();
    private static final AtomicLong ID_GENERATOR = new AtomicLong(1L);

    private final RestTemplate restTemplate;
    private final String applicationServiceUrl;

    public JobController(RestTemplate restTemplate,
                         @Value("${application-service.url:http://localhost:8084}") String applicationServiceUrl) {
        this.restTemplate = restTemplate;
        this.applicationServiceUrl = applicationServiceUrl;
    }

    static {
        createSeedJob(1L, "Senior Product Engineer", "Nexus AI", "San Francisco, CA", "Full-time", "5-8 years", 160000, 220000,
                "Lead the architecture and development of core AI infrastructure.", List.of("TypeScript", "React", "Node.js", "AWS"));
        createSeedJob(2L, "Principal Systems Architect", "Orbital Systems", "Seattle, WA", "Full-time", "10+ years", 220000, 300000,
                "Architect the next generation of flight software systems.", List.of("C++", "RTOS", "Embedded Systems", "Linux"));
        createSeedJob(3L, "Clinical Data Scientist", "Meridian Health", "Boston, MA", "Full-time", "3-5 years", 110000, 150000,
                "Apply advanced data-science methods to clinical trial data.", List.of("Python", "R", "SQL", "Biostatistics"));
    }

    @GetMapping("/jobs")
    public List<Map<String, Object>> getJobs() {
        return new ArrayList<>(JOBS.values());
    }

    @GetMapping("/jobs/{id}")
    public Map<String, Object> getJob(@PathVariable Long id) {
        Map<String, Object> job = JOBS.get(id);
        if (job == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found");
        }
        return job;
    }

    @PostMapping("/jobs")
    public Map<String, Object> createJob(@RequestBody Map<String, Object> payload) {
        Long id = ID_GENERATOR.getAndIncrement();
        Map<String, Object> job = buildJobFromPayload(id, payload);
        JOBS.put(id, job);
        return job;
    }

    @PutMapping("/jobs/{id}")
    public Map<String, Object> updateJob(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Map<String, Object> existing = JOBS.get(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found");
        }

        Map<String, Object> updated = buildJobFromPayload(id, payload);
        JOBS.put(id, updated);
        return updated;
    }

    @PostMapping("/jobs/{id}/apply")
    public Map<String, Object> applyToJob(@PathVariable Long id,
                                          @RequestBody(required = false) Map<String, Object> applicant) {
        Map<String, Object> job = JOBS.get(id);
        if (job == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found");
        }

        Map<String, Object> application = new HashMap<>();
        if (applicant != null) {
            application.putAll(applicant);
        }
        application.put("jobId", id);
        application.put("jobTitle", job.get("title"));
        Object company = job.get("company");
        Object companyName = company instanceof Map<?, ?> companyMap ? companyMap.get("name") : company;
        application.put("companyName", companyName == null ? "TalentSync" : String.valueOf(companyName));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> created = restTemplate.postForObject(
                applicationServiceUrl + "/api/applications", new HttpEntity<>(application, headers), Map.class);
        return created == null ? Map.of("jobId", id, "status", "APPLIED") : created;
    }

    @GetMapping("/user/saved-jobs")
    public List<Map<String, Object>> getSavedJobs() {
        return List.of(JOBS.get(1L), JOBS.get(2L));
    }

    @PostMapping("/user/saved-jobs/{id}")
    public Map<String, String> saveJob(@PathVariable Long id) {
        if (JOBS.get(id) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found");
        }
        return Map.of("status", "saved");
    }

    @DeleteMapping("/user/saved-jobs/{id}")
    public Map<String, String> removeSavedJob(@PathVariable Long id) {
        if (JOBS.get(id) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found");
        }
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
        JOBS.put(id, job);
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
        return job;
    }
}

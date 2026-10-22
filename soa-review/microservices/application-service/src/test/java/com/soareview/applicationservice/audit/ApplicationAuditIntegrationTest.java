package com.soareview.applicationservice.audit;

import com.soareview.applicationservice.security.ServiceJwtFilter;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ApplicationAuditIntegrationTest {

    private static final String JWT_SECRET = "test-jwt-secret-key-for-service-tests-2026";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ServiceJwtFilter serviceJwtFilter;

    private MockMvc mockMvc;

    @Autowired
    private AuditEventRepository auditRepository;

    @MockBean
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(serviceJwtFilter)
                .build();
        auditRepository.deleteAll();
        when(restTemplate.exchange(eq("http://AUTH-SERVICE/api/user/profile"), eq(HttpMethod.GET),
                any(HttpEntity.class), eq(Map.class))).thenReturn(ResponseEntity.ok(Map.of(
                "id", 420L,
                "name", "Audit Candidate",
                "email", "audit-candidate@example.invalid",
                "professionalTitle", "Engineer",
                "location", "Remote",
                "bio", "Integration test profile")));
    }

    @Test
    void recordsApplicantCreateReadAndRecruiterStatusUpdate() throws Exception {
        String applicantToken = bearer("audit-candidate@example.invalid", "USER", 420);
        String recruiterToken = bearer("recruiter@demo.com", "RECRUITER", 710);

        mockMvc.perform(get("/api/applications")
                        .header("Authorization", applicantToken)
                        .header("X-User-Email", "attacker@example.invalid")
                        .header("X-User-Role", "ADMIN")
                        .header("X-User-Id", "999")
                        .header("X-Correlation-ID", "untrusted-header-value"))
                .andExpect(status().isOk());

        String response = mockMvc.perform(post("/api/applications")
                        .header("Authorization", applicantToken)
                        .header("X-User-Email", "attacker@example.invalid")
                        .header("X-User-Role", "ADMIN")
                        .header("X-User-Id", "999")
                        .contentType("application/json")
                        .content("{\"jobId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andReturn().getResponse().getContentAsString();
        long applicationId = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readTree(response).get("id").asLong();

        mockMvc.perform(patch("/api/applications/" + applicationId + "/status")
                        .header("Authorization", recruiterToken)
                        .header("X-User-Email", "attacker@example.invalid")
                        .header("X-User-Role", "USER")
                        .header("X-User-Id", "999")
                        .contentType("application/json")
                        .content("{\"status\":\"shortlisted\"}"))
                .andExpect(status().isOk());

        assertThat(auditRepository.findAll())
                .anySatisfy(event -> {
                    assertThat(event.getActorId()).isEqualTo("420");
                    assertThat(event.getActorRole()).isEqualTo("USER");
                    assertThat(event.getOperation()).isEqualTo("CREATE");
                    assertThat(event.getResourceType()).isEqualTo("APPLICATION");
                    assertThat(event.getOutcome()).isEqualTo("SUCCESS");
                })
                .anySatisfy(event -> {
                    assertThat(event.getActorId()).isEqualTo("420");
                    assertThat(event.getOperation()).isEqualTo("READ");
                    assertThat(event.getResourceType()).isEqualTo("APPLICATION");
                    assertThat(event.getCorrelationId()).matches(
                            "(?i)[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}");
                    assertThat(event.getCorrelationId()).doesNotContain("untrusted-header-value");
                })
                .anySatisfy(event -> {
                    assertThat(event.getActorId()).isEqualTo("710");
                    assertThat(event.getActorRole()).isEqualTo("RECRUITER");
                    assertThat(event.getOperation()).isEqualTo("UPDATE");
                    assertThat(event.getOutcome()).isEqualTo("SUCCESS");
                });
    }

    private String bearer(String email, String role, long actorId) {
        Instant now = Instant.now();
        return "Bearer " + Jwts.builder()
                .subject(email)
                .claim("role", role)
                .claim("verified", true)
                .claim("actorId", actorId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(600)))
                .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}

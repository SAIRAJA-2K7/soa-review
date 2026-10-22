package com.soareview.jobservice.audit;

import com.soareview.jobservice.persistence.JobDocumentStore;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JobAuditIntegrationTest {

    private static final String JWT_SECRET = "test-jwt-secret-key-for-service-tests-2026";

    @Autowired
    private MockMvc mockMvc;

    @SpyBean
    private AuditEventRepository auditRepository;

    @Autowired
    private JobDocumentStore documentStore;

    @MockBean
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        auditRepository.deleteAll();
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of()));
    }

    @Test
    void recordsTrustedActorForJobCreateAndAuthenticatedRead() throws Exception {
        mockMvc.perform(post("/api/jobs")
                        .header("Authorization", bearer("recruiter@example.invalid", "RECRUITER", 71))
                        .header("X-User-Email", "attacker@example.invalid")
                        .header("X-User-Role", "ADMIN")
                        .header("X-User-Id", "999")
                        .header("X-Correlation-ID", "untrusted-header-value")
                        .contentType("application/json")
                        .content("""
                                {"title":"Audited Role","companyName":"Example Org","location":"Remote",
                                 "description":"A role for the audit integration test"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/jobs")
                        .header("Authorization", bearer("candidate@example.invalid", "USER", 42))
                        .header("X-User-Email", "attacker@example.invalid")
                        .header("X-User-Role", "ADMIN")
                        .header("X-User-Id", "999"))
                .andExpect(status().isOk());

        assertThat(auditRepository.findAll())
                .anySatisfy(event -> {
                    assertThat(event.getActorId()).isEqualTo("71");
                    assertThat(event.getActorRole()).isEqualTo("RECRUITER");
                    assertThat(event.getOperation()).isEqualTo("CREATE");
                    assertThat(event.getResourceType()).isEqualTo("JOB");
                    assertThat(event.getResourceId()).isNotBlank();
                    assertThat(event.getOutcome()).isEqualTo("SUCCESS");
                    assertThat(event.getChangedFields()).contains("title", "companyName", "description");
                    assertThat(event.getCorrelationId()).matches(
                            "(?i)[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}");
                    assertThat(event.getCorrelationId()).doesNotContain("untrusted-header-value");
                })
                .anySatisfy(event -> {
                    assertThat(event.getActorId()).isEqualTo("42");
                    assertThat(event.getActorRole()).isEqualTo("USER");
                    assertThat(event.getOperation()).isEqualTo("READ");
                    assertThat(event.getOutcome()).isEqualTo("SUCCESS");
                });
    }

    @Test
    void recordsRejectedJobMutationWithoutPersistingAFalseSuccess() throws Exception {
        mockMvc.perform(post("/api/jobs")
                        .header("Authorization", bearer("recruiter@example.invalid", "RECRUITER", 71))
                        .header("X-User-Email", "attacker@example.invalid")
                        .header("X-User-Role", "ADMIN")
                        .header("X-User-Id", "999")
                        .contentType("application/json")
                        .content("""
                                {"title":"Invalid Role","companyName":"","location":"Remote","description":"Invalid"}
                                """))
                .andExpect(status().isBadRequest());

        assertThat(auditRepository.findAll())
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.getActorId()).isEqualTo("71");
                    assertThat(event.getOperation()).isEqualTo("CREATE");
                    assertThat(event.getOutcome()).isEqualTo("FAILURE");
                    assertThat(event.getHttpStatus()).isEqualTo(400);
                    assertThat(event.getErrorCategory()).isEqualTo("REQUEST_REJECTED");
                });
    }

    @Test
    void rollsBackJobMutationWhenBothSuccessAndFailureAuditWritesFail() throws Exception {
        doThrow(new DataIntegrityViolationException("simulated audit persistence failure"))
                .when(auditRepository).saveAndFlush(any(AuditEvent.class));

        assertThatThrownBy(() -> mockMvc.perform(post("/api/jobs")
                        .header("Authorization", bearer("recruiter@example.invalid", "RECRUITER", 71))
                        .contentType("application/json")
                        .content("""
                                {"title":"Audit Failure Role","companyName":"Example Org","location":"Remote",
                                 "description":"A role for audit failure verification"}
                                """)))
                .isInstanceOf(ServletException.class)
                .hasRootCauseInstanceOf(DataIntegrityViolationException.class);

        assertThat(documentStore.findAll("jobs"))
                .noneMatch(job -> "Audit Failure Role".equals(job.get("title")));
        assertThat(auditRepository.findAll()).isEmpty();
        verify(auditRepository, times(2)).saveAndFlush(any(AuditEvent.class));
    }

    private String bearer(String email, String role, long actorId) {
        Instant now = Instant.now();
        String token = Jwts.builder()
                .subject(email)
                .claim("role", role)
                .claim("verified", true)
                .claim("actorId", actorId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(600)))
                .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
        return "Bearer " + token;
    }
}

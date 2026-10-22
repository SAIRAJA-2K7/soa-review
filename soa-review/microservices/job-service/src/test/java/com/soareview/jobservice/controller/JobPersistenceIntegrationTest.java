package com.soareview.jobservice.controller;

import com.soareview.jobservice.persistence.JobDocumentStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@SpringBootTest
class JobPersistenceIntegrationTest {

    @Autowired
    private JobController jobController;

    @Autowired
    private JobDocumentStore documentStore;

    @MockBean
    private RestTemplate restTemplate;

    @BeforeEach
    void stubApplicationServiceSync() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of()));
    }

    @Test
    void seedsJobsAndPersistsRecruiterOwnedJobs() {
        assertThat(jobController.getJobs()).hasSize(3);

        Map<String, Object> created = jobController.createJob(Map.of(
                        "title", "Persistence Test Engineer",
                        "companyName", "Example Co",
                        "location", "Remote",
                        "description", "Build reliable services"),
                        "recruiter@example.com", "RECRUITER", "Bearer test-token");

        Long id = ((Number) created.get("id")).longValue();
        assertThat(documentStore.find("jobs", String.valueOf(id))).containsEntry("title", "Persistence Test Engineer");
        assertThat(jobController.getRecruiterJobs("recruiter@example.com", "RECRUITER"))
            .anySatisfy(job -> assertThat(((Number) job.get("id")).longValue()).isEqualTo(id));
    }

    @Test
    void rejectsNonRecruitersFromCreatingJobs() {
        assertThatThrownBy(() -> jobController.createJob(Map.of("title", "forbidden"), "candidate@example.com", "USER", "Bearer test-token"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void returnsSavedJobsFromBatchLookup() {
        jobController.saveJob(1L, "saved-candidate@example.com", "USER");

        assertThat(jobController.getSavedJobs("saved-candidate@example.com", "USER"))
                .extracting(job -> String.valueOf(job.get("id")))
                .containsExactly("1");
    }

            @Test
            void rejectsInvalidJobFieldsInsteadOfFailingDuringMapping() {
            assertThatThrownBy(() -> jobController.createJob(Map.of(
                    "title", "Engineer",
                    "companyName", "",
                    "location", "Remote",
                    "description", "Details"),
                "recruiter@example.com", "RECRUITER", "Bearer test-token"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

            assertThatThrownBy(() -> jobController.createJob(Map.of(
                    "title", "Engineer",
                    "companyName", "Example Co",
                    "location", "Remote",
                    "description", "Details",
                    "salaryMin", 200,
                    "salaryMax", 100),
                "recruiter@example.com", "RECRUITER", "Bearer test-token"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
            }

            @Test
            void rollsBackCreatedJobWhenApplicationServiceSyncFails() {
            when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new ResourceAccessException("application service unavailable"));

            assertThatThrownBy(() -> jobController.createJob(Map.of(
                    "title", "Sync Failure Role",
                    "companyName", "Example Co",
                    "location", "Remote",
                    "description", "Details"),
                "recruiter@example.com", "RECRUITER", "Bearer test-token"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(documentStore.findAll("jobs"))
                .noneMatch(job -> "Sync Failure Role".equals(job.get("title")));
            }

            @Test
                void applicationForwardingUsesCandidateEmailFromVerifiedGatewayHeader() {
            when(restTemplate.postForObject(eq("http://localhost:8084/api/applications"),
                any(HttpEntity.class), eq(Map.class))).thenReturn(Map.of("id", 77L));

            jobController.applyToJob(1L, Map.of(
                    "candidateId", 900L,
                    "candidateName", "Forged Name",
                    "candidateEmail", "forged@example.com"),
                "candidate@example.com", "USER", "Bearer verified-token");

            ArgumentCaptor<HttpEntity<Map>> forwarded = ArgumentCaptor.forClass(HttpEntity.class);
            verify(restTemplate).postForObject(eq("http://localhost:8084/api/applications"), forwarded.capture(), eq(Map.class));
            assertThat(forwarded.getValue().getBody())
                .containsEntry("candidateEmail", "candidate@example.com")
                .containsEntry("jobId", 1L)
                .doesNotContainKey("candidateName")
                .doesNotContainKey("candidateId");
            }
}
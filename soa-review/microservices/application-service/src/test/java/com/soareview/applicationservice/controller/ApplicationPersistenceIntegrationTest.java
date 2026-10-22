package com.soareview.applicationservice.controller;

import com.soareview.applicationservice.persistence.ApplicationDocumentStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
class ApplicationPersistenceIntegrationTest {

    @Autowired
    private ApplicationController applicationController;

    @Autowired
    private ApplicationDocumentStore documentStore;

        @MockBean
        private RestTemplate restTemplate;

        private void stubCandidateProfile(String email, long id, String name) {
                when(restTemplate.exchange(eq("http://AUTH-SERVICE/api/user/profile"), eq(HttpMethod.GET),
                                any(HttpEntity.class), eq(Map.class))).thenReturn(ResponseEntity.ok(Map.of(
                                "id", id,
                                "name", name,
                                "email", email,
                                "professionalTitle", "Engineer",
                                "location", "Remote",
                                "bio", "Verified profile")));
        }

    @Test
    void persistsApplicationsAndScopesCandidateReadsByEmail() {
                stubCandidateProfile("new-candidate@example.com", 900L, "New Candidate");
        Map<String, Object> application = applicationController.createApplication(Map.of(
                "candidateEmail", "new-candidate@example.com",
                "jobId", 1,
                                "jobTitle", "Forged Title"), "new-candidate@example.com", "Bearer test-token");

        Long id = ((Number) application.get("id")).longValue();
        assertThat(documentStore.find("applications", String.valueOf(id)))
                .containsEntry("candidateEmail", "new-candidate@example.com");
        assertThat(applicationController.getApplications("new-candidate@example.com"))
                .anySatisfy(record -> assertThat(((Number) record.get("id")).longValue()).isEqualTo(id));
        assertThat(applicationController.getApplications("someone-else@example.com"))
                .noneMatch(record -> id.equals(record.get("id")));
    }

    @Test
    void rejectsDuplicateApplicationsAndIgnoresForgedCandidateFields() {
        stubCandidateProfile("duplicate@example.com", 42L, "Canonical Candidate");
        Map<String, Object> request = Map.of(
                "candidateEmail", "forged@example.com",
                "candidateId", 999L,
                "candidateName", "Forged Candidate",
                "jobId", 1,
                "jobTitle", "Example Role");
        Map<String, Object> created = applicationController.createApplication(
                request, "duplicate@example.com", "Bearer test-token");
        assertThat(created)
                .containsEntry("candidateEmail", "duplicate@example.com")
                .containsEntry("candidateId", 42L)
                .containsEntry("candidateName", "Canonical Candidate");

        assertThatThrownBy(() -> applicationController.createApplication(
                request, "duplicate@example.com", "Bearer test-token"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        stubCandidateProfile("other@example.com", 43L, "Other Candidate");
        assertThatThrownBy(() -> applicationController.createApplication(
                request, "duplicate@example.com", "Bearer different-token"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void databaseReservationAllowsOnlyOneConcurrentApplication() throws Exception {
        String key = "concurrent-" + UUID.randomUUID() + ":1";
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Boolean> first = executor.submit(() -> createReservation(key, ready, start));
            Future<Boolean> second = executor.submit(() -> createReservation(key, ready, start));

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(first.get(10, TimeUnit.SECONDS)).isNotEqualTo(second.get(10, TimeUnit.SECONDS));
        }
    }

    private boolean createReservation(String key, CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting to start concurrent application attempts");
        }
        try {
            documentStore.createUnique("applications", "application-reservations", key,
                    new java.util.LinkedHashMap<>(Map.of("candidateEmail", "locked@example.com", "jobId", 1)));
            return true;
        } catch (DataIntegrityViolationException exception) {
            return false;
        }
    }

        @Test
        void internalJobSyncRequiresServiceSecret() {
                assertThatThrownBy(() -> applicationController.syncRecruiterJob(88L,
                                Map.of("title", "Synced role"), "recruiter@example.com", "wrong-secret"))
                                .isInstanceOf(ResponseStatusException.class)
                                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                                .isEqualTo(HttpStatus.FORBIDDEN);

                Map<String, Object> synced = applicationController.syncRecruiterJob(88L,
                                Map.of("title", "Synced role"), "recruiter@example.com", "test-job-sync-secret-for-service-tests-2026");
                assertThat(synced).containsEntry("id", 88L).containsEntry("recruiterEmail", "recruiter@example.com");
        }
}
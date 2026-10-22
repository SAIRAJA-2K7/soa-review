package com.soareview.jobservice.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "SOA_REVIEW_POSTGRES_TEST_URL", matches = "jdbc:postgresql://.+")
class PostgresPersistenceIntegrationTest {

    @Autowired
    private JobDocumentStore documentStore;

    @Autowired
    private JobDocumentRepository repository;

    @DynamicPropertySource
    static void configureDedicatedPostgresDatabase(DynamicPropertyRegistry registry) {
        String url = requiredTestDatabaseUrl();
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> requiredEnvironment("SOA_REVIEW_POSTGRES_TEST_USERNAME"));
        registry.add("spring.datasource.password", () -> requiredEnvironment("SOA_REVIEW_POSTGRES_TEST_PASSWORD"));
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registry.add("eureka.client.enabled", () -> false);
    }

    @Test
    void persistsAndReadsDocumentsUsingPostgres() {
        String key = "postgres-test-" + UUID.randomUUID();
        try {
            documentStore.save("postgres-integration-tests", key, Map.of("result", "persisted"));
            assertThat(documentStore.find("postgres-integration-tests", key))
                    .containsEntry("result", "persisted");
        } finally {
            repository.findByCollectionNameAndRecordKey("postgres-integration-tests", key)
                    .ifPresent(repository::delete);
        }
    }

    private static String requiredTestDatabaseUrl() {
        String url = requiredEnvironment("SOA_REVIEW_POSTGRES_TEST_URL");
        String database = url.substring(url.lastIndexOf('/') + 1).split("\\?", 2)[0];
        if (!database.matches("(?i).+_(test|integration)")) {
            throw new IllegalStateException("PostgreSQL integration tests require a dedicated database ending in _test or _integration.");
        }
        return url;
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Set " + name + " to run PostgreSQL integration tests.");
        }
        return value;
    }
}

package com.soareview.authservice.service;

import com.soareview.authservice.repository.OtpChallengeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OtpChallengeStoreTest {

    @Autowired
    private OtpChallengeRepository repository;

    private OtpChallengeStore store;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        store = new OtpChallengeStore(repository, 300, 5);
    }

    @Test
    void verifiesCaseInsensitiveEmailAndConsumesCodeOnce() {
        store.issue("Candidate@example.com", "123456");

        assertThat(store.verifyAndConsume("candidate@example.com", "123456")).isTrue();
        assertThat(store.verifyAndConsume("candidate@example.com", "123456")).isFalse();
    }

    @Test
    void removesChallengeAfterMaximumFailedAttempts() {
        store = new OtpChallengeStore(repository, 300, 2);
        store.issue("candidate@example.com", "123456");

        assertThat(store.verifyAndConsume("candidate@example.com", "000000")).isFalse();
        assertThat(store.verifyAndConsume("candidate@example.com", "000000")).isFalse();
        assertThat(store.verifyAndConsume("candidate@example.com", "123456")).isFalse();
    }

    @Test
    void rejectsExpiredChallenges() throws InterruptedException {
        store = new OtpChallengeStore(repository, 1, 5);
        store.issue("candidate@example.com", "123456");
        Thread.sleep(1_100);

        assertThat(store.verifyAndConsume("candidate@example.com", "123456")).isFalse();
    }
}

package com.soareview.authservice.service;

import com.soareview.authservice.entity.AppUser;
import com.soareview.authservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserServicePersistenceTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldPersistUserAndFindByEmail() {
        AppUser user = new AppUser();
        user.setName("Alice");
        user.setEmail("alice@example.com");
        user.setPassword("secret123");
        user.setRole("USER");
        user.setVerified(true);
        user.setOnboarded(false);

        userRepository.save(user);

        assertThat(userRepository.findByEmail("alice@example.com")).isPresent();
    }
}

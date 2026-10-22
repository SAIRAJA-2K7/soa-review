package com.soareview.authservice.config;

import com.soareview.authservice.entity.AppUser;
import com.soareview.authservice.repository.UserRepository;
import com.soareview.authservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:demo-seed-development;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "eureka.client.enabled=false",
        "management.health.mail.enabled=false",
        "app.demo-users.enabled=true",
        "demo-users.candidate.email=",
        "demo-users.candidate.password=",
        "demo-users.recruiter.email=demo-recruiter@example.invalid",
        "demo-users.recruiter.password=TestOnly-Recruiter-Password-871!"
})
@ActiveProfiles("dev")
class DemoUserInitializerDevelopmentTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void explicitlyEnabledDevelopmentSeedCreatesRecruiterWithoutOverwritingExistingAccounts() {
        AppUser recruiter = userRepository.findByEmail("demo-recruiter@example.invalid").orElseThrow();
        assertThat(recruiter.getRole()).isEqualTo("RECRUITER");
        assertThat(recruiter.getRequestedRole()).isNull();
        assertThat(recruiter.isVerified()).isTrue();
        assertThat(passwordEncoder.matches("TestOnly-Recruiter-Password-871!", recruiter.getPassword())).isTrue();
        assertThat(userRepository.findByEmail("demo-admin@example.invalid")).isEmpty();

        String originalPasswordHash = recruiter.getPassword();
        userService.ensureDemoUsers("", "", recruiter.getEmail(), "A-Different-Test-Password-764!");
        assertThat(userRepository.findById(recruiter.getId()).orElseThrow().getPassword())
                .isEqualTo(originalPasswordHash);
    }
}

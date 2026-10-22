package com.soareview.authservice.config;

import com.soareview.authservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:demo-seed-profile;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "eureka.client.enabled=false",
        "management.health.mail.enabled=false",
        "app.demo-users.enabled=true"
})
@ActiveProfiles("prod")
class DemoUserInitializerProfileTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private UserRepository userRepository;

    @Test
    void productionProfileDoesNotLoadDemoSeedInitializerEvenWhenEnabled() {
        assertThat(applicationContext.containsBean("demoUserInitializer")).isFalse();
        assertThat(userRepository.findByEmail("candidate@demo.com")).isEmpty();
        assertThat(userRepository.findByEmail("recruiter@demo.com")).isEmpty();
        assertThat(userRepository.findByEmail("admin@demo.com")).isEmpty();
    }
}

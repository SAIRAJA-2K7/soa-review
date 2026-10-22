package com.soareview.authservice.config;

import com.soareview.authservice.service.UserService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "test"})
@ConditionalOnProperty(name = "app.demo-users.enabled", havingValue = "true")
public class DemoUserInitializer implements ApplicationRunner {

    private final UserService userService;
    private final String candidateEmail;
    private final String candidatePassword;
    private final String recruiterEmail;
    private final String recruiterPassword;

    public DemoUserInitializer(UserService userService,
                               @Value("${demo-users.candidate.email:}") String candidateEmail,
                               @Value("${demo-users.candidate.password:}") String candidatePassword,
                               @Value("${demo-users.recruiter.email:}") String recruiterEmail,
                               @Value("${demo-users.recruiter.password:}") String recruiterPassword) {
        this.userService = userService;
        this.candidateEmail = candidateEmail;
        this.candidatePassword = candidatePassword;
        this.recruiterEmail = recruiterEmail;
        this.recruiterPassword = recruiterPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        userService.ensureDemoUsers(candidateEmail, candidatePassword, recruiterEmail, recruiterPassword);
    }
}

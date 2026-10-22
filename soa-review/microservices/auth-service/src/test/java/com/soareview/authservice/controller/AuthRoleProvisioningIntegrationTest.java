package com.soareview.authservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soareview.authservice.config.JwtUtil;
import com.soareview.authservice.entity.AppUser;
import com.soareview.authservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth-role-provisioning;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "eureka.client.enabled=false",
        "management.health.mail.enabled=false",
        "app.demo-users.enabled=false"
})
@AutoConfigureMockMvc
class AuthRoleProvisioningIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void candidateRegistrationCannotChoosePrivilegedRole() throws Exception {
        String email = newEmail();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "name", "Candidate",
                                "email", email,
                                "password", randomPassword(),
                                "role", "USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.requestedRole").value(nullValue()));

        AppUser saved = userRepository.findByEmail(email).orElseThrow();
        assertThat(saved.getRole()).isEqualTo("USER");
        assertThat(saved.getRequestedRole()).isNull();
        assertThat(saved.isVerified()).isFalse();

        String forgedRoleEmail = newEmail();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "name", "Forged Role",
                                "email", forgedRoleEmail,
                                "password", randomPassword(),
                                "role", "SUPERADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"));
        assertThat(userRepository.findByEmail(forgedRoleEmail).orElseThrow().getRequestedRole()).isNull();

        String adminRequestEmail = newEmail();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "name", "Unapproved Admin Request",
                                "email", adminRequestEmail,
                                "password", randomPassword(),
                                "role", "ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"));
        assertThat(userRepository.findByEmail(adminRequestEmail).orElseThrow().getRole()).isEqualTo("USER");
    }

    @Test
    void recruiterCanRegisterDirectlyWithoutReceivingAdminPrivileges() throws Exception {
        String email = newEmail();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "name", "Recruiter Applicant",
                                "email", email,
                                "password", randomPassword(),
                                "role", "RECRUITER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("RECRUITER"))
                .andExpect(jsonPath("$.requestedRole").value(nullValue()))
                .andExpect(jsonPath("$.verified").value(false));

        AppUser saved = userRepository.findByEmail(email).orElseThrow();
        assertThat(saved.getRole()).isEqualTo("RECRUITER");
        assertThat(saved.getRequestedRole()).isNull();
        assertThat(saved.isVerified()).isFalse();
    }

    @Test
    void adminApprovalApiIsUnavailableAndProfileCannotEscalateRole() throws Exception {
        AppUser candidate = createUser("USER", true);
        String candidateToken = jwtUtil.generateToken(candidate.getId(), candidate.getEmail(), "USER", true);

        mockMvc.perform(get("/api/auth/admin/recruiter-requests")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/user/profile")
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "role", "ADMIN",
                                "requestedRole", "ADMIN",
                                "name", "Still a candidate"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"));

        assertThat(userRepository.findById(candidate.getId()).orElseThrow().getRole()).isEqualTo("USER");
        mockMvc.perform(get("/api/auth/admin/recruiter-requests"))
                .andExpect(status().isForbidden());
    }

    private AppUser createUser(String role, boolean verified) {
        AppUser user = new AppUser();
        user.setName("Role test " + role);
        user.setEmail(newEmail());
        user.setPassword(passwordEncoder.encode(randomPassword()));
        user.setRole(role);
        user.setVerified(verified);
        user.setActive(true);
        user.setOnboarded(false);
        return userRepository.saveAndFlush(user);
    }

    private String newEmail() {
        return "role-test-" + UUID.randomUUID() + "@example.invalid";
    }

    private String randomPassword() {
        return UUID.randomUUID() + "Aa1!";
    }
}

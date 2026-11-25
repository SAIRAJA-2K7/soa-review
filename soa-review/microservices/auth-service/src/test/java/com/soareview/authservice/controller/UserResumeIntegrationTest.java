package com.soareview.authservice.controller;

import com.soareview.authservice.config.JwtUtil;
import com.soareview.authservice.entity.AppUser;
import com.soareview.authservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:user-resume-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "eureka.client.enabled=false",
        "management.health.mail.enabled=false",
        "app.demo-users.enabled=false"
})
@AutoConfigureMockMvc
class UserResumeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void resumeCanBeUploadedDownloadedAndDeletedByItsOwner() throws Exception {
        AppUser user = createVerifiedCandidate();
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), "USER", true);
        byte[] pdf = "%PDF-1.7\nresume contents\n%%EOF".getBytes(StandardCharsets.US_ASCII);
        MockMultipartFile file = new MockMultipartFile("file", "candidate-resume.pdf",
                MediaType.APPLICATION_PDF_VALUE, pdf);

        mockMvc.perform(multipart("/api/user/resume").file(file)
                        .header("Authorization", "Bearer " + token)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("candidate-resume.pdf"))
                .andExpect(jsonPath("$.size").value(pdf.length));

        mockMvc.perform(get("/api/user/resume").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", containsString("candidate-resume.pdf")))
                .andExpect(content().bytes(pdf));

        mockMvc.perform(delete("/api/user/resume").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/user/resume").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void resumeUploadRequiresAuthenticationAndValidPdfContent() throws Exception {
        MockMultipartFile nonPdf = new MockMultipartFile("file", "fake.pdf",
                MediaType.APPLICATION_PDF_VALUE, "not a pdf".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/user/resume").file(nonPdf)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isUnauthorized());

        AppUser user = createVerifiedCandidate();
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), "USER", true);
        mockMvc.perform(multipart("/api/user/resume").file(nonPdf)
                        .header("Authorization", "Bearer " + token)
                        .with(request -> {
                                    request.setMethod("PUT");
                                    return request;
                                }))
                        .andExpect(status().isBadRequest());
    }

    private AppUser createVerifiedCandidate() {
        String email = "resume-" + UUID.randomUUID() + "@example.invalid";
        AppUser user = new AppUser();
        user.setName("Resume Candidate");
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(UUID.randomUUID() + "Aa1!"));
        user.setRole("USER");
        user.setVerified(true);
        user.setActive(true);
        user.setOnboarded(false);
        return userRepository.saveAndFlush(user);
    }
}

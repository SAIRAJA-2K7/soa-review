package com.soareview.backend.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JobControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldExposePublicJobsEndpoint() throws Exception {
        mockMvc.perform(get("/api/jobs"))
            .andExpect(status().isOk());
    }

    @Test
    void shouldReturnRecruiterDashboardDataForAuthenticatedAdmin() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"recruiter@demo.com\",\"password\":\"password123\"}"))
            .andExpect(status().isOk())
            .andReturn();

        String token = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.token");

        mockMvc.perform(get("/api/admin/dashboard")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activeJobs").exists())
            .andExpect(jsonPath("$.jobs").isArray())
            .andExpect(jsonPath("$.applications").isArray())
            .andExpect(jsonPath("$.scheduledInterviews").isArray());
    }

    @Test
    void shouldIncludeNewlyCreatedJobInRecruiterJobList() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"recruiter@demo.com\",\"password\":\"password123\"}"))
            .andExpect(status().isOk())
            .andReturn();

        String token = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.token");

        String payload = """
            {
              "title": "Frontend Engineer",
              "companyName": "Northwind Labs",
              "location": "Remote",
              "type": "REMOTE",
              "experience": "2-4 years",
              "salaryMin": 120000,
              "salaryMax": 160000,
              "description": "Build product experiences for enterprise customers.",
              "responsibilities": ["Ship polished UIs"],
              "requirements": ["React and TypeScript"],
              "skills": ["React", "TypeScript"]
            }
            """;

        mockMvc.perform(post("/api/jobs")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/jobs")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].title").value(org.hamcrest.Matchers.hasItem("Frontend Engineer")));
    }
}

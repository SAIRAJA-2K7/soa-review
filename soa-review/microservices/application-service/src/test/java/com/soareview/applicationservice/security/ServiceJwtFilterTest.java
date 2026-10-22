package com.soareview.applicationservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ServiceJwtFilterTest {

    private static final String SECRET = "test-jwt-secret-key-for-service-tests-2026";
    private ServiceJwtFilter filter;

    @BeforeEach
    void setUp() {
        filter = new ServiceJwtFilter();
        ReflectionTestUtils.setField(filter, "secret", SECRET);
    }

    @Test
    void rejectsRequestsWithoutToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/applications");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void rejectsUnverifiedTokens() throws Exception {
        MockHttpServletRequest request = authenticatedRequest("GET", "/api/admin/applications", "ADMIN", false);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void blocksCandidatesFromRecruiterEndpoints() throws Exception {
        MockHttpServletRequest request = authenticatedRequest("GET", "/api/admin/applications", "USER");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void allowsRecruitersAndRejectsAdminsFromRecruiterEndpoints() throws Exception {
        MockHttpServletResponse recruiterResponse = new MockHttpServletResponse();
        FilterChain recruiterChain = mock(FilterChain.class);
        filter.doFilter(authenticatedRequest("GET", "/api/admin/applications", "RECRUITER"),
                recruiterResponse, recruiterChain);
        verify(recruiterChain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        assertThat(recruiterResponse.getStatus()).isEqualTo(HttpStatus.OK.value());

        MockHttpServletResponse adminResponse = new MockHttpServletResponse();
        filter.doFilter(authenticatedRequest("GET", "/api/admin/applications", "ADMIN"),
                adminResponse, mock(FilterChain.class));
        assertThat(adminResponse.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void replacesUntrustedIdentityHeaders() throws Exception {
        MockHttpServletRequest request = authenticatedRequest("GET", "/api/admin/applications", "RECRUITER");
        request.addHeader("X-User-Email", "forged@example.com");
        request.addHeader("X-User-Role", "USER");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        var forwarded = org.mockito.ArgumentCaptor.forClass(jakarta.servlet.ServletRequest.class);
        verify(chain).doFilter(forwarded.capture(), org.mockito.ArgumentMatchers.any());
        var forwardedRequest = (jakarta.servlet.http.HttpServletRequest) forwarded.getValue();
        assertThat(forwardedRequest.getHeader("X-User-Email")).isEqualTo("verified@example.com");
        assertThat(forwardedRequest.getHeader("X-User-Role")).isEqualTo("RECRUITER");
        assertThat(java.util.Collections.list(forwardedRequest.getHeaders("X-User-Email")))
                .containsExactly("verified@example.com");
        assertThat(java.util.Collections.list(forwardedRequest.getHeaders("X-User-Role")))
                .containsExactly("RECRUITER");
        assertThat(java.util.Collections.list(forwardedRequest.getHeaders("X-User-Id")))
                .containsExactly("verified@example.com");
        assertThat(java.util.Collections.list(forwardedRequest.getHeaderNames()))
                .contains("X-User-Id");
    }

    private MockHttpServletRequest authenticatedRequest(String method, String path, String role) {
        return authenticatedRequest(method, path, role, true);
    }

    private MockHttpServletRequest authenticatedRequest(String method, String path, String role, boolean verified) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.addHeader("Authorization", "Bearer " + Jwts.builder()
                .subject("verified@example.com")
                .claim("role", role)
                .claim("verified", verified)
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact());
        return request;
    }
}
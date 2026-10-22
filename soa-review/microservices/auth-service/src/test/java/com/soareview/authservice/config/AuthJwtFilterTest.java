package com.soareview.authservice.config;

import com.soareview.authservice.entity.AppUser;
import com.soareview.authservice.service.UserService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthJwtFilterTest {

    private static final String SECRET = "test-jwt-secret-key-for-service-tests-2026";
    private UserService userService;
    private AuthJwtFilter filter;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        filter = new AuthJwtFilter(jwtUtil, userService);
    }

    @Test
    void rejectsRequestsWithoutToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/profile");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        verify(chain, never()).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsTamperedTokens() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/profile");
        request.addHeader("Authorization", "Bearer " + token("USER", "other-secret-key-not-valid-for-signing-2026"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        verify(userService, never()).findByEmail(anyString());
    }

    @Test
    void replacesUntrustedIdentityHeadersForActiveUser() throws Exception {
        AppUser user = new AppUser();
        user.setEmail("candidate@example.com");
        user.setRole("USER");
        user.setActive(true);
        user.setVerified(true);
        when(userService.findByEmail("candidate@example.com")).thenReturn(user);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/profile");
        request.addHeader("Authorization", "Bearer " + token("USER", SECRET));
        request.addHeader("X-User-Email", "forged@example.com");
        request.addHeader("X-User-Role", "ADMIN");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        var forwarded = org.mockito.ArgumentCaptor.forClass(jakarta.servlet.ServletRequest.class);
        verify(chain).doFilter(forwarded.capture(), org.mockito.ArgumentMatchers.any());
        var forwardedRequest = (jakarta.servlet.http.HttpServletRequest) forwarded.getValue();
        assertThat(forwardedRequest.getHeader("X-User-Email")).isEqualTo("candidate@example.com");
        assertThat(forwardedRequest.getHeader("X-User-Role")).isEqualTo("USER");
        assertThat(java.util.Collections.list(forwardedRequest.getHeaders("X-User-Email")))
                .containsExactly("candidate@example.com");
        assertThat(java.util.Collections.list(forwardedRequest.getHeaders("X-User-Role")))
                .containsExactly("USER");
        assertThat(java.util.Collections.list(forwardedRequest.getHeaderNames()))
                .contains("X-User-Id");
    }

    @Test
    void rejectsInactiveUsersEvenWithValidToken() throws Exception {
        AppUser user = new AppUser();
        user.setEmail("candidate@example.com");
        user.setRole("USER");
        user.setActive(false);
        user.setVerified(true);
        when(userService.findByEmail("candidate@example.com")).thenReturn(user);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/profile");
        request.addHeader("Authorization", "Bearer " + token("USER", SECRET));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        verify(chain, never()).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void allowsAuthEndpointsWithoutToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void rejectsUnverifiedUsersWithValidToken() throws Exception {
        AppUser user = new AppUser();
        user.setEmail("candidate@example.com");
        user.setRole("USER");
        user.setActive(true);
        user.setVerified(false);
        when(userService.findByEmail("candidate@example.com")).thenReturn(user);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/profile");
        request.addHeader("Authorization", "Bearer " + token("USER", SECRET));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        verify(chain, never()).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private String token(String role, String key) {
        return Jwts.builder()
                .subject("candidate@example.com")
                .claim("role", role)
                .claim("verified", true)
                .signWith(Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
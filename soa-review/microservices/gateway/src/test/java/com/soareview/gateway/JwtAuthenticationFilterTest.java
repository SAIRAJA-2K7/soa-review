package com.soareview.gateway;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "test-jwt-secret-key-for-gateway-tests-2026";
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter();
        ReflectionTestUtils.setField(filter, "secret", SECRET);
    }

    @Test
    void rejectsRequestsWithoutBearerToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/admin/applications").build());

        filter.filter(exchange, ignored -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsValidlySignedTokensForUnverifiedUsers() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/admin/applications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("USER", false))
                        .build());

        filter.filter(exchange, ignored -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsExpiredTokens() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/admin/applications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken())
                        .build());

        filter.filter(exchange, ignored -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsCandidateAccessToRecruiterRoutes() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/admin/applications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("USER"))
                        .build());

        filter.filter(exchange, ignored -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void replacesUntrustedIdentityHeadersWithVerifiedClaims() {
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/admin/applications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("RECRUITER"))
                        .header("X-User-Email", "attacker@example.com")
                        .header("X-User-Role", "ADMIN")
                        .build());

        filter.filter(exchange, request -> {
            forwarded.set(request);
            return Mono.empty();
        }).block();

        assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-User-Email"))
                .isEqualTo("recruiter@example.com");
        assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-User-Role"))
                .isEqualTo("RECRUITER");
    }

    @Test
    void limitsSensitiveOperationsByRole() {
        assertThat(JwtAuthenticationFilter.isAuthorized(HttpMethod.POST, "/api/jobs", "USER")).isFalse();
        assertThat(JwtAuthenticationFilter.isAuthorized(HttpMethod.POST, "/api/jobs", "RECRUITER")).isTrue();
        assertThat(JwtAuthenticationFilter.isAuthorized(HttpMethod.POST, "/api/jobs", "ADMIN")).isFalse();
        assertThat(JwtAuthenticationFilter.isAuthorized(HttpMethod.POST, "/api/jobs/7/apply", "USER")).isTrue();
        assertThat(JwtAuthenticationFilter.isAuthorized(HttpMethod.POST, "/api/jobs/7/apply", "RECRUITER")).isFalse();
        assertThat(JwtAuthenticationFilter.isAuthorized(HttpMethod.PATCH, "/api/applications/7/status", "USER")).isFalse();
        assertThat(JwtAuthenticationFilter.isAuthorized(
                HttpMethod.POST, "/api/admin/applications", "RECRUITER")).isTrue();
        assertThat(JwtAuthenticationFilter.isAuthorized(
                HttpMethod.POST, "/api/admin/applications", "ADMIN")).isFalse();
        assertThat(JwtAuthenticationFilter.isAuthorized(
                HttpMethod.POST, "/api/auth/admin/accounts/7/approve-recruiter", "USER")).isFalse();
        assertThat(JwtAuthenticationFilter.isAuthorized(
                HttpMethod.POST, "/api/auth/admin/accounts/7/approve-recruiter", "RECRUITER")).isFalse();
        assertThat(JwtAuthenticationFilter.isAuthorized(
                HttpMethod.POST, "/api/auth/admin/accounts/7/approve-recruiter", "ADMIN")).isFalse();
        assertThat(JwtAuthenticationFilter.isPublicAuthEndpoint("/api/auth/register")).isTrue();
        assertThat(JwtAuthenticationFilter.isPublicAuthEndpoint("/api/auth/admin/recruiter-requests")).isFalse();
        assertThat(JwtAuthenticationFilter.isPublicAuthEndpoint("/api/auth/unknown")).isFalse();
    }

    private String token(String role) {
        return token(role, true);
    }

    private String token(String role, boolean verified) {
        return Jwts.builder()
                .subject("recruiter@example.com")
                .claim("role", role)
                .claim("verified", verified)
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private String expiredToken() {
        Date issuedAt = new Date(System.currentTimeMillis() - 60_000);
        return Jwts.builder()
                .subject("recruiter@example.com")
                .claim("role", "ADMIN")
                .claim("verified", true)
                .issuedAt(issuedAt)
                .expiration(new Date(issuedAt.getTime() - 1_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
package com.soareview.jobservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

@Component
public class ServiceJwtFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String secret;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/") ||
            (HttpMethod.GET.matches(request.getMethod()) &&
                ("/api/jobs".equals(path) || path.matches("/api/jobs/\\d+")) &&
                request.getHeader(HttpHeaders.AUTHORIZATION) == null);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(getSigningKey()).build()
                    .parseSignedClaims(authorization.substring(7)).getPayload();
        } catch (Exception exception) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        String email = claims.getSubject();
        String role = String.valueOf(claims.get("role"));
        if (email == null || email.isBlank() || !Boolean.TRUE.equals(claims.get("verified", Boolean.class)) ||
            !("ADMIN".equals(role) || "RECRUITER".equals(role) || "USER".equals(role))) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }
        boolean publicJobRead = HttpMethod.GET.matches(request.getMethod()) &&
                ("/api/jobs".equals(request.getRequestURI()) || request.getRequestURI().matches("/api/jobs/\\d+"));
        if (!publicJobRead && !isAuthorized(request.getMethod(), request.getRequestURI(), role)) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            return;
        }

        request.setAttribute("authenticatedEmail", email);
        request.setAttribute("authenticatedRole", role);
        String actorId = claims.get("actorId") == null ? email : String.valueOf(claims.get("actorId"));
        var wrapped = new jakarta.servlet.http.HttpServletRequestWrapper(request) {
            private String authenticatedHeader(String name) {
                if ("X-User-Email".equalsIgnoreCase(name)) return email;
                if ("X-User-Role".equalsIgnoreCase(name)) return role;
                if ("X-User-Id".equalsIgnoreCase(name)) return actorId;
                return null;
            }

            @Override
            public String getHeader(String name) {
                String value = authenticatedHeader(name);
                return value == null ? super.getHeader(name) : value;
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                String value = authenticatedHeader(name);
                return value == null ? super.getHeaders(name) : Collections.enumeration(List.of(value));
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                List<String> names = new ArrayList<>();
                Enumeration<String> originalNames = super.getHeaderNames();
                if (originalNames != null) {
                    while (originalNames.hasMoreElements()) {
                        String name = originalNames.nextElement();
                        if (authenticatedHeader(name) == null) names.add(name);
                    }
                }
                names.addAll(List.of("X-User-Email", "X-User-Role", "X-User-Id"));
                return Collections.enumeration(names);
            }
        };
        chain.doFilter(wrapped, response);
    }

    static boolean isAuthorized(String method, String path, String role) {
        if (path.startsWith("/api/admin/")) return "RECRUITER".equals(role);
        if ((HttpMethod.POST.matches(method) || HttpMethod.PUT.matches(method)) && "/api/jobs".equals(path)) {
            return "RECRUITER".equals(role);
        }
        if (HttpMethod.PUT.matches(method) && path.matches("/api/jobs/\\d+")) return "RECRUITER".equals(role);
        if (HttpMethod.POST.matches(method) && path.matches("/api/jobs/\\d+/apply")) return "USER".equals(role);
        if (path.startsWith("/api/user/saved-jobs")) return "USER".equals(role);
        return "USER".equals(role) || "RECRUITER".equals(role);
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
package com.soareview.authservice.config;

import com.soareview.authservice.entity.AppUser;
import com.soareview.authservice.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

@Component
public class AuthJwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserService userService;

    public AuthJwtFilter(JwtUtil jwtUtil, UserService userService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        String email;
        String role;
        String actorId;
        try {
            String token = authorization.substring(7);
            if (!jwtUtil.validateToken(token)) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                return;
            }
            email = jwtUtil.extractEmail(token);
            role = jwtUtil.extractRole(token);
            AppUser user = userService.findByEmail(email);
            if (user == null || !user.isActive() || !user.isVerified() || !role.equals(user.getRole())) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                return;
            }
            actorId = String.valueOf(user.getId());
        } catch (Exception exception) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        var authentication = new UsernamePasswordAuthenticationToken(
                email, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
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
}
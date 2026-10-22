package com.soareview.gateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Value("${jwt.secret}")
    private String secret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        boolean publicJobRead = isPublicJobRead(exchange.getRequest().getMethod(), path);
        boolean publicAuthEndpoint = isPublicAuthEndpoint(path);
        if (publicAuthEndpoint || "/actuator/health".equals(path) ||
            (publicJobRead && exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION) == null)) {
            return chain.filter(exchange);
        }

        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authorization.substring(7);
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

                if (claims.getSubject() == null || claims.get("role") == null ||
                    !Boolean.TRUE.equals(claims.get("verified", Boolean.class))) {
                throw new RuntimeException("Invalid JWT claims");
            }

            String role = String.valueOf(claims.get("role"));
            if (!"ADMIN".equals(role) && !"RECRUITER".equals(role) && !"USER".equals(role)) {
                throw new RuntimeException("Unsupported JWT role");
            }
            if (!publicJobRead && !isAuthorized(exchange.getRequest().getMethod(), path, role)) {
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(exchange.getRequest().mutate()
                            .headers(headers -> {
                                headers.remove("X-User-Email");
                                headers.remove("X-User-Role");
                                headers.remove("X-User-Id");
                                headers.set("X-User-Email", claims.getSubject());
                                headers.set("X-User-Role", role);
                                headers.set("X-User-Id", claims.get("actorId") == null
                                        ? claims.getSubject() : String.valueOf(claims.get("actorId")));
                            })
                            .build())
                    .build();

            return chain.filter(mutatedExchange);
        } catch (Exception ex) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }

    static boolean isAuthorized(HttpMethod method, String path, String role) {
        if (path.startsWith("/api/auth/admin/")) return false;
        if ("/api/admin".equals(path) || path.startsWith("/api/admin/")) {
            return "RECRUITER".equals(role);
        }

        if ((HttpMethod.POST.equals(method) || HttpMethod.PUT.equals(method)) && "/api/jobs".equals(path)) {
            return "RECRUITER".equals(role);
        }
        if (HttpMethod.PUT.equals(method) && path.matches("/api/jobs/\\d+")) {
            return "RECRUITER".equals(role);
        }
        if (HttpMethod.POST.equals(method) && path.matches("/api/jobs/\\d+/apply")) {
            return "USER".equals(role);
        }
        if (HttpMethod.PATCH.equals(method) && path.matches("/api/applications/\\d+/status")) {
            return "RECRUITER".equals(role);
        }
        if (HttpMethod.POST.equals(method) && "/api/applications".equals(path)) {
            return false;
        }
        if (path.equals("/api/applications") || path.matches("/api/applications/\\d+")) {
            return "USER".equals(role);
        }

        return "ADMIN".equals(role) || "RECRUITER".equals(role) || "USER".equals(role);
    }

    static boolean isPublicJobRead(HttpMethod method, String path) {
        return HttpMethod.GET.equals(method) &&
                ("/api/jobs".equals(path) || PATH_MATCHER.match("/api/jobs/{id}", path));
    }

    static boolean isPublicAuthEndpoint(String path) {
        return switch (path) {
            case "/api/auth/health", "/api/auth/register", "/api/auth/login",
                    "/api/auth/request-otp", "/api/auth/verify-otp",
                    "/api/auth/password-reset/request", "/api/auth/password-reset/consume" -> true;
            default -> false;
        };
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}

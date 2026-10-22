package com.soareview.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayHealthAccessTest {

    @Test
    void permitsOnlyTheHealthProbeWithoutJwt() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/actuator/health").build());
        AtomicBoolean forwarded = new AtomicBoolean();

        filter.filter(exchange, ignored -> {
            forwarded.set(true);
            return Mono.empty();
        }).block();

        assertThat(forwarded).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void allowsPublicJobReadsWithoutJwtButNotWrites() {
        assertThat(JwtAuthenticationFilter.isPublicJobRead(
                org.springframework.http.HttpMethod.GET, "/api/jobs")).isTrue();
        assertThat(JwtAuthenticationFilter.isPublicJobRead(
                org.springframework.http.HttpMethod.GET, "/api/jobs/12")).isTrue();
        assertThat(JwtAuthenticationFilter.isPublicJobRead(
                org.springframework.http.HttpMethod.POST, "/api/jobs/12/apply")).isFalse();
    }
}
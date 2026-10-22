package com.soareview.jobservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class InstanceDiagnosticHeaderFilter extends OncePerRequestFilter {

    private final boolean enabled;
    private final String instanceId;

    public InstanceDiagnosticHeaderFilter(
            @Value("${load-balancing.instance-header-enabled:false}") boolean enabled,
            @Value("${spring.application.name:job-service}") String serviceName,
            @Value("${server.port:8083}") String port) {
        this.enabled = enabled;
        this.instanceId = serviceName + ":" + port;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (enabled) {
            response.setHeader("X-Service-Instance", instanceId);
        }
        filterChain.doFilter(request, response);
    }
}

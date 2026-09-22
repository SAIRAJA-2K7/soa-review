package com.soareview.gateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class GatewayController {

    @GetMapping("/")
    public Map<String, Object> home() {
        return Map.of(
                "service", "TalentSync API Gateway",
                "status", "UP",
                "routes", Map.of(
                        "auth", "/api/auth/**",
                        "jobs", "/api/jobs/**",
                        "applications", "/api/applications/**"
                )
        );
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}

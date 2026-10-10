package com.campusflow.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {
    @GetMapping({"/health", "/api/v1/health"})
    public Map<String, Object> health() {
        return Map.of("status", "UP", "service", "campusflow-backend", "time", Instant.now().toString());
    }
}

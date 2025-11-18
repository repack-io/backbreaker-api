package com.repackio.backbreaker.api;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
public class HealthController {

    @PostConstruct
    public void init() {
        log.info("========================================");
        log.info("HealthController initialized successfully");
        log.info("Health endpoint available at /");
        log.info("========================================");
    }

    @GetMapping("/")
    public ResponseEntity<Map<String, String>> health() {
        log.debug("Health check requested");
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("application", "backbreaker");
        return ResponseEntity.ok(response);
    }
}

package br.ufpb.dsc.jobhub.controller;

import br.ufpb.dsc.jobhub.service.DatabaseHealthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class PingController {

    private final DatabaseHealthService databaseHealthService;

    public PingController(DatabaseHealthService databaseHealthService) {
        this.databaseHealthService = databaseHealthService;
    }

    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        try {
            if (!databaseHealthService.isDatabaseAvailable()) {
                return unavailable();
            }
            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "service", "eq13",
                    "database", "up",
                    "timestamp", Instant.now().toString()
            ));
        } catch (Exception ignored) {
            return unavailable();
        }
    }

    private ResponseEntity<Map<String, Object>> unavailable() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", "error",
                        "service", "eq13",
                        "database", "down",
                        "timestamp", Instant.now().toString()
                ));
    }
}

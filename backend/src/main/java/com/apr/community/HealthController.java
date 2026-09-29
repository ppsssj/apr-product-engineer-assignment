package com.apr.community;

import java.util.Map;

import javax.sql.DataSource;

import com.apr.community.seed.SeedReadiness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final DataSource dataSource;
    private final SeedReadiness seedReadiness;

    public HealthController(DataSource dataSource, SeedReadiness seedReadiness) {
        this.dataSource = dataSource;
        this.seedReadiness = seedReadiness;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        if (!seedReadiness.isReady()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("status", "starting", "db", "up"));
        }

        try (var connection = dataSource.getConnection()) {
            connection.getMetaData().getDatabaseProductVersion();
            return ResponseEntity.ok(Map.of("status", "ok", "db", "up"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("status", "down", "db", "unreachable"));
        }
    }
}

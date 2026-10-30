package com.reconengine.controller;

import com.reconengine.repository.TransactionRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    private final DataSource dataSource;
    private final TransactionRepository repository;

    public HealthController(DataSource dataSource, TransactionRepository repository) {
        this.dataSource = dataSource;
        this.repository = repository;
    }

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("time", Instant.now().toString());
        body.put("java", System.getProperty("java.version"));
        try (Connection c = dataSource.getConnection()) {
            body.put("database", c.getMetaData().getDatabaseProductName() + " " + c.getMetaData().getDatabaseProductVersion());
            body.put("transactions", repository.count());
        } catch (Exception e) {
            body.put("status", "DEGRADED");
            body.put("database", "unreachable: " + e.getMessage());
        }
        return body;
    }
}

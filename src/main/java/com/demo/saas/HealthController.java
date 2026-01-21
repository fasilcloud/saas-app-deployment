package com.demo.saas;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;

@RestController
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // ✅ Simple hello endpoint (NO DB)
    @GetMapping("/hello")
    public String hello() {
        return "Hello World";
    }

    // ✅ Health endpoint (DB check)
    @GetMapping("/health")
    public String health() {
        try (Connection conn = dataSource.getConnection()) {
            return "OK - App & DB Connected";
        } catch (Exception e) {
            return "DB Connection Failed";
        }
    }
}


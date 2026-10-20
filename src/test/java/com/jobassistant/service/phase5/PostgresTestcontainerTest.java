package com.jobassistant.service.phase5;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * PostgreSQL Testcontainers integration test for Phase 5.
 * <p>
 * Uses a disposable PostgreSQL container with real Flyway migrations (V1..V11)
 * applied to a fresh database. This validates that every migration applies
 * cleanly against PostgreSQL — the H2-based tests cover entity/SQL dialect
 * behavior, but cannot prove the PostgreSQL-specific DDL.
 * <p>
 * Requires a Docker runtime. When Docker is unavailable the container
 * start fails (real environment blocker, not a false pass). Re-enable
 * by providing Docker (DOCKER_HOST / Docker Desktop).
 */
@Testcontainers
@SpringBootTest
public class PostgresTestcontainerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("testdb")
        .withUsername("sa")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Test
    public void postgresContainerStartsAndMigrationsApply() {
        // Context loads => Flyway V1..V11 applied to fresh PostgreSQL DB.
    }
}

package com.repackio.backbreaker.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

@Slf4j
@Configuration
public class DatabaseConnectionLogger {

    private final Environment environment;
    private final DataSource dataSource;

    @Value("${spring.datasource.url:NOT_SET}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:NOT_SET}")
    private String datasourceUsername;

    @Value("${DB_SECRET_NAME:NOT_SET}")
    private String secretName;

    public DatabaseConnectionLogger(Environment environment, DataSource dataSource) {
        this.environment = environment;
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void logDatabaseConnection() {
        log.info("========================================");
        log.info("DATABASE CONNECTION CONFIGURATION");
        log.info("========================================");
        log.info("Secret Name (from env): {}", secretName);
        log.info("Datasource URL: {}", datasourceUrl);
        log.info("Datasource Username: {}", datasourceUsername);
        log.info("Active Profiles: {}", String.join(", ", environment.getActiveProfiles()));

        try {
            log.info("Testing database connection...");
            var connection = dataSource.getConnection();
            var metadata = connection.getMetaData();
            log.info("✓ Database connection successful!");
            log.info("  Database Product: {}", metadata.getDatabaseProductName());
            log.info("  Database Version: {}", metadata.getDatabaseProductVersion());
            log.info("  Driver: {}", metadata.getDriverName());
            log.info("  Driver Version: {}", metadata.getDriverVersion());
            connection.close();
            log.info("========================================");
        } catch (Exception e) {
            log.error("========================================");
            log.error("✗ DATABASE CONNECTION FAILED!");
            log.error("  Exception: {}", e.getClass().getName());
            log.error("  Message: {}", e.getMessage());
            log.error("========================================");
            log.error("Full exception details:", e);
        }
    }
}

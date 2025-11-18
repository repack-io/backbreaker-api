package com.repackio.backbreaker.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.profiles.active", havingValue = "aws")
public class SecretsManagerLogger {

    private final Environment environment;

    @Value("${DB_SECRET_NAME:NOT_SET}")
    private String secretName;

    @Value("${host:NOT_LOADED}")
    private String dbHost;

    @Value("${port:NOT_LOADED}")
    private String dbPort;

    @Value("${dbname:NOT_LOADED}")
    private String dbName;

    @Value("${username:NOT_LOADED}")
    private String dbUsername;

    public SecretsManagerLogger(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void logSecretsManagerConfig() {
        log.info("========================================");
        log.info("SECRETS MANAGER CONFIGURATION");
        log.info("========================================");
        log.info("Secret Name (DB_SECRET_NAME env var): {}", secretName);
        log.info("spring.config.import: {}", environment.getProperty("spring.config.import", "NOT_SET"));
        log.info("");
        log.info("Loaded database credentials:");
        log.info("  host: {}", dbHost);
        log.info("  port: {}", dbPort);
        log.info("  dbname: {}", dbName);
        log.info("  username: {}", dbUsername);
        log.info("  password: {}", maskPassword(environment.getProperty("password", "NOT_LOADED")));
        log.info("========================================");
    }

    private String maskPassword(String password) {
        if (password == null || password.equals("NOT_LOADED")) {
            return password;
        }
        if (password.length() <= 4) {
            return "****";
        }
        return password.substring(0, 2) + "****" + password.substring(password.length() - 2);
    }
}

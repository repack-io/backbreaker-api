package com.repackio.backbreaker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class BackbreakerApplication {

    public static void main(String[] args) {
        log.info("======== BACKBREAKER APPLICATION STARTING ========");
        log.info("Java version: {}", System.getProperty("java.version"));
        log.info("SPRING_PROFILES_ACTIVE: {}", System.getenv("SPRING_PROFILES_ACTIVE"));
        log.info("DB_SECRET_NAME: {}", System.getenv("DB_SECRET_NAME"));
        log.info("AWS_REGION: {}", System.getenv("AWS_REGION"));
        log.info("Active profiles will be determined by SPRING_PROFILES_ACTIVE environment variable");

        try {
            SpringApplication.run(BackbreakerApplication.class, args);
            log.info("======== BACKBREAKER APPLICATION STARTED SUCCESSFULLY ========");
        } catch (Exception e) {
            log.error("======== FATAL: APPLICATION STARTUP FAILED ========", e);
            log.error("Exception type: {}", e.getClass().getName());
            log.error("Exception message: {}", e.getMessage());
            throw e;
        }
    }
}

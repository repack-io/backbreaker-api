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

        // Set default profile if not specified
        String profilesActive = System.getenv("SPRING_PROFILES_ACTIVE");
        String activeProfile;
        if (profilesActive == null || profilesActive.isBlank()) {
            activeProfile = "local-dev";
            log.info("No SPRING_PROFILES_ACTIVE set, defaulting to '{}'", activeProfile);
        } else {
            activeProfile = profilesActive;
            log.info("SPRING_PROFILES_ACTIVE: {}", activeProfile);
        }

        log.info("DB_SECRET_NAME: {}", System.getenv("DB_SECRET_NAME"));
        log.info("AWS_REGION: {}", System.getenv("AWS_REGION"));

        try {
            SpringApplication app = new SpringApplication(BackbreakerApplication.class);
            app.setAdditionalProfiles(activeProfile);
            app.run(args);
            log.info("======== BACKBREAKER APPLICATION STARTED SUCCESSFULLY ========");
        } catch (Exception e) {
            log.error("======== FATAL: APPLICATION STARTUP FAILED ========", e);
            log.error("Exception type: {}", e.getClass().getName());
            log.error("Exception message: {}", e.getMessage());
            throw e;
        }
    }
}

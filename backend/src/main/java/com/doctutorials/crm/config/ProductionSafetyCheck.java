package com.doctutorials.crm.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Refuses to boot in the "prod" profile with an unsafe JWT secret.
 * Dev and test are unaffected.
 */
@Component
@Profile("prod")
public class ProductionSafetyCheck {

    private static final String DEFAULT_JWT_SECRET =
            "change-this-to-a-long-random-secret-in-every-real-environment";

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @PostConstruct
    void verify() {
        if (jwtSecret == null || jwtSecret.equals(DEFAULT_JWT_SECRET) || jwtSecret.length() < 32) {
            throw new IllegalStateException(
                    "Unsafe JWT secret in prod: set CRM_JWT_SECRET to a random value of at least 32 characters.");
        }
    }
}
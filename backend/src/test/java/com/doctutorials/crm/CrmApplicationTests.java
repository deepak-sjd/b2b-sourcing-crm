package com.doctutorials.crm;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test: fails fast in CI if the Spring context can't wire up
 * (bad bean definitions, missing config, etc.) before anything reaches production.
 */
@SpringBootTest
@ActiveProfiles("test")
class CrmApplicationTests {

    @Test
    void contextLoads() {
        // Intentionally empty: Spring throws if the ApplicationContext fails to start.
    }
}

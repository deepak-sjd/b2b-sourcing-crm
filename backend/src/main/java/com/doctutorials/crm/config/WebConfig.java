package com.doctutorials.crm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;
import org.springframework.data.web.config.SortHandlerMethodArgumentResolverCustomizer;

/**
 * Every controller in this app takes a plain Spring Data {@code Pageable}
 * straight from query params (see LeadController, TaskController,
 * CallController, ...). Without a global cap, {@code ?size=2000000} is a
 * one-line way for any caller to force a full-table load - exactly the kind
 * of thing that's invisible in dev with a handful of seed rows and becomes an
 * incident the first week a client has 200k+ leads. This customizer applies
 * to every {@code Pageable} argument app-wide, so no controller has to
 * remember to clamp it individually.
 */
@Configuration
public class WebConfig {

    @Value("${app.pagination.default-page-size:20}")
    private int defaultPageSize;

    @Value("${app.pagination.max-page-size:100}")
    private int maxPageSize;

    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer() {
        return resolver -> {
            resolver.setFallbackPageable(
                    org.springframework.data.domain.PageRequest.of(0, defaultPageSize));
            resolver.setMaxPageSize(maxPageSize);
            resolver.setOneIndexedParameters(false);
        };
    }

    @Bean
    public SortHandlerMethodArgumentResolverCustomizer sortCustomizer() {
        // No-op today, but declared explicitly so a future change to allowed sort
        // properties (e.g. an allow-list to block sorting by a non-indexed column)
        // has one obvious place to live instead of being bolted onto a random service.
        return sortResolver -> { };
    }
}

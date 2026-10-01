package com.doctutorials.crm.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caches ReportService's full-table aggregations (lead-grouping report,
 * counselor performance) for a short TTL.
 *
 * Why cache at all: {@code counselorPerformance()} and {@code runLeadReport()}
 * scan every visible lead every call - fine at seed-data scale, a real cost
 * once a client has 50k+ leads and a dashboard auto-refreshing every few
 * seconds. Why Caffeine (in-memory) and not Redis: these numbers are
 * per-organization read models where "up to 30s stale" is a completely
 * acceptable trade for not hitting Postgres on every dashboard paint - not
 * data that needs to be shared across app instances or survive a restart. If
 * this app ever runs more than one instance behind a load balancer AND the
 * report numbers need to be consistent across instances, swap this for a
 * Redis-backed CacheManager - the {@code @Cacheable} annotations on
 * ReportService don't change either way.
 *
 * Deliberately NOT wired with @CacheEvict on every lead mutation: that would
 * mean touching LeadService.create/update/changeStage/assign and every other
 * write path just to keep a 30-second cache honest, which is a lot of new
 * coupling for a report that's allowed to be eventually consistent anyway.
 * The TTL alone keeps staleness bounded and predictable.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String REPORTS_CACHE = "reportsCache";

    @Bean
    public CacheManagerCustomizer<CaffeineCacheManager> reportsCacheCustomizer() {
        return cacheManager -> cacheManager.registerCustomCache(
                REPORTS_CACHE,
                Caffeine.newBuilder()
                        .expireAfterWrite(30, TimeUnit.SECONDS)
                        .maximumSize(1_000) // bounded so a huge fan-out of distinct groupBy/role combos can't grow unbounded
                        .build());
    }
}

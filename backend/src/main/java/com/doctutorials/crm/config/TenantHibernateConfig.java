package com.doctutorials.crm.config;

import com.doctutorials.crm.tenant.TenantContext;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class TenantHibernateConfig {

    @Bean
    CurrentTenantIdentifierResolver<UUID> tenantIdentifierResolver() {
        return new CurrentTenantIdentifierResolver<>() {
            @Override
            public UUID resolveCurrentTenantIdentifier() {
                return TenantContext.currentOrSystem();
            }

            @Override
            public boolean validateExistingCurrentSessions() {
                return true;
            }
        };
    }

    @Bean
    HibernatePropertiesCustomizer tenantHibernateCustomizer(CurrentTenantIdentifierResolver<UUID> resolver) {
        return props -> props.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, resolver);
    }
}
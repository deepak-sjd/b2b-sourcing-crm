package com.doctutorials.crm.tenant;

import com.doctutorials.crm.organization.Organization;
import com.doctutorials.crm.organization.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Consumer;

/** Runs a scheduled job once per active tenant. One tenant failing never stops the others. */
@Component
@RequiredArgsConstructor
public class TenantRunner {

    private static final Logger log = LoggerFactory.getLogger(TenantRunner.class);

    private final OrganizationRepository organizationRepository;

    public void forEachActiveTenant(Consumer<UUID> action) {
        for (Organization org : organizationRepository.findAll()) {
            if (!org.isActive()) {
                continue;
            }
            try {
                TenantContext.runAs(org.getId(), () -> action.accept(org.getId()));
            } catch (RuntimeException e) {
                log.error("Scheduled job failed for tenant {}", org.getId(), e);
            }
        }
    }
}
package com.doctutorials.crm.lead;

import com.doctutorials.crm.tenant.TenantRunner;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Nightly rescore of open leads, run once per tenant. */
@Component
@RequiredArgsConstructor
public class LeadAiRefreshJob {

    private static final Logger log = LoggerFactory.getLogger(LeadAiRefreshJob.class);

    private final LeadService leadService;
    private final TenantRunner tenantRunner;

    @Scheduled(cron = "0 15 2 * * *")
    public void refreshOpenLeads() {
        int[] total = {0};
        tenantRunner.forEachActiveTenant(id -> total[0] += leadService.refreshAllOpenLeadsForSystemJob());
        if (total[0] > 0) {
            log.info("Nightly lead AI refresh: rescored {} open leads", total[0]);
        }
    }
}
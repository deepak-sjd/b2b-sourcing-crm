package com.doctutorials.crm.lead;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * The score/intent/AI-summary shown on a lead is only ever recomputed when
 * someone touches that specific lead (create/update/stage-change/touchpoint).
 * A lead nobody has opened in two weeks still needs its "days since contact"
 * signal to age and its score to decay — otherwise a lead that went quiet
 * looks exactly as healthy today as the day it was last touched, and the
 * at-risk banner / score badge silently go stale.
 *
 * This runs once nightly (02:15 server time — low-traffic window) over every
 * open (non-archived, non-closed) lead org-wide and re-runs the same
 * rescore + AI-insight logic a manual edit would trigger, so "score just
 * dropped overnight because 7 days passed" and "AI panel already has a fresh
 * recommendation the next morning" both just happen without anyone asking.
 */
@Component
@RequiredArgsConstructor
public class LeadAiRefreshJob {

    private static final Logger log = LoggerFactory.getLogger(LeadAiRefreshJob.class);

    private final LeadService leadService;

    @Scheduled(cron = "0 15 2 * * *")
    public void refreshOpenLeads() {
        int updated = leadService.refreshAllOpenLeadsForSystemJob();
        if (updated > 0) {
            log.info("Nightly lead AI refresh: rescored {} open leads", updated);
        }
    }
}

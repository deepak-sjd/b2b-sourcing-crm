package com.doctutorials.crm.quotation;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Nightly job that marks quotations whose validUntil date has passed as EXPIRED.
 * The actual work (and its @Transactional boundary + audit logging) lives in
 * QuotationService.expireOverdueQuotationsForSystemJob().
 */
@Component
@RequiredArgsConstructor
public class QuotationExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(QuotationExpiryJob.class);

    private final QuotationService quotationService;

    /** Every day at 00:30 server time. */
    @Scheduled(cron = "0 30 0 * * *")
    public void expireOverdueQuotations() {
        int expired = quotationService.expireOverdueQuotationsForSystemJob();
        if (expired > 0) {
            log.info("Quotation expiry: marked {} quotation(s) as EXPIRED", expired);
        }
    }
}
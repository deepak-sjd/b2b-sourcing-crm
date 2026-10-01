package com.doctutorials.crm.customer;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * AI automation #4 — same "deterministic, explainable" philosophy as
 * LeadScoringService / AiReportingAssistantService elsewhere in this
 * codebase: a short, rule-based health note rather than an LLM call, so it's
 * instant, free to generate, and "why does it say that" always has a
 * one-line answer. Regenerated on every create/update — see CustomerService.
 */
@Service
public class CustomerInsightService {

    public String generateHealthSummary(Customer customer) {
        StringBuilder sb = new StringBuilder();

        switch (customer.getApprovalStatus()) {
            case PENDING -> sb.append("Awaiting approval. ");
            case REJECTED -> sb.append("Registration was rejected — review before any further activity. ");
            case APPROVED -> sb.append("Approved. ");
        }

        long daysSinceCreated = customer.getCreatedAt() == null
                ? 0
                : ChronoUnit.DAYS.between(customer.getCreatedAt(), Instant.now());

        if (daysSinceCreated <= 3) {
            sb.append("Just onboarded.");
        } else if (customer.getContacts().isEmpty()) {
            sb.append("No contacts on file after ").append(daysSinceCreated).append(" days — worth a follow-up call.");
        } else {
            sb.append(customer.getContacts().size()).append(" contact(s) on file.");
        }

        if (customer.getAccountManager() == null) {
            sb.append(" Unassigned — no account manager yet.");
        }

        return sb.toString().trim();
    }
}

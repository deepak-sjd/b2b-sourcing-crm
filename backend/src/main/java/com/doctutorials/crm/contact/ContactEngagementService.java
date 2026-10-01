package com.doctutorials.crm.contact;

import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Recomputes engagementScore / engagement / aiInsight whenever a contact is
 * created, edited, or touched. Deliberately a transparent, rule-based model —
 * same reasoning as lead.LeadScoringService: a CRM user needs to be able to
 * say "why does this contact look at-risk", not just trust a number. Swap
 * this for a trained model later behind the same recompute(Contact) call.
 */
@Service
public class ContactEngagementService {

    private static final long ACTIVE_WITHIN_DAYS = 3;
    private static final long COOLING_WITHIN_DAYS = 14;

    public void recompute(Contact contact) {
        long daysSince = contact.getDaysSinceLastActivity();
        int score;
        ContactEngagement engagement;

        if (daysSince < 0) {
            score = 40;
            engagement = ContactEngagement.NEW;
        } else if (daysSince <= ACTIVE_WITHIN_DAYS) {
            score = 90;
            engagement = ContactEngagement.ACTIVE;
        } else if (daysSince <= COOLING_WITHIN_DAYS) {
            score = 60;
            engagement = ContactEngagement.COOLING;
        } else {
            score = 25;
            engagement = ContactEngagement.AT_RISK;
        }

        // Data-completeness signal — a contact we can actually reach is worth more.
        if (contact.getEmail() != null && !contact.getEmail().isBlank()) score += 8;
        if (contact.getPhone() != null && !contact.getPhone().isBlank()) score += 8;

        if (contact.isEmailBounced()) {
            score -= 30;
            engagement = ContactEngagement.AT_RISK;
        }

        contact.setEngagementScore(Math.max(0, Math.min(100, score)));
        contact.setEngagement(engagement);
        contact.setAiInsight(buildInsight(contact, daysSince));
    }

    public void markTouched(Contact contact) {
        contact.setLastActivityAt(Instant.now());
        recompute(contact);
    }

    private String buildInsight(Contact contact, long daysSince) {
        if (contact.isEmailBounced()) {
            return "The email on file bounced — confirm a working address before the next send.";
        }
        boolean missingEmail = contact.getEmail() == null || contact.getEmail().isBlank();
        boolean missingPhone = contact.getPhone() == null || contact.getPhone().isBlank();

        if (daysSince < 0) {
            if (missingEmail && missingPhone) {
                return "New contact with no phone or email yet — add a way to reach them.";
            }
            return "New contact, no activity logged yet — reach out to get things started.";
        }
        if (daysSince <= ACTIVE_WITHIN_DAYS) {
            return "Engaged recently (" + daysSince + "d ago) — good time to move the conversation forward.";
        }
        if (daysSince <= COOLING_WITHIN_DAYS) {
            return "No activity in " + daysSince + " days — a quick check-in keeps this warm.";
        }
        if (missingEmail || missingPhone) {
            return "Inactive for " + daysSince + " days and missing " + (missingEmail ? "an email" : "a phone number")
                    + " — hard to re-engage without it.";
        }
        return "Inactive for " + daysSince + " days — this contact is going cold, follow up soon.";
    }
}

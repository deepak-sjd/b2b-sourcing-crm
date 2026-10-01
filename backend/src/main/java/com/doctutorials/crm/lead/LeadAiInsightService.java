package com.doctutorials.crm.lead;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

/**
 * Generates the "AI Summary" and "Next Best Action" shown on the Lead detail
 * page's AI Insight panel.
 *
 * Deliberately rule-based over the lead's own fields — same reasoning as
 * LeadScoringService and AiReportingAssistantService: an internal CRM handling
 * real contact PII shouldn't pipe lead data into a raw third-party LLM call
 * (prompt-injection via free-text fields like `interest`/`city`, and data
 * exfiltration risk). This produces a genuinely useful, always-available
 * summary with zero external dependency and zero latency, and every line of
 * it is traceable back to a concrete field — nothing "why did it say that".
 *
 * If a real LLM is wired in later (e.g. to turn this into more natural prose,
 * or to draft outreach copy), keep this class as the deterministic fallback
 * and call it when the LLM call fails or times out, so the panel never goes
 * blank.
 */
@Service
public class LeadAiInsightService {

    private static final DateTimeFormatter DAY_FORMAT =
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.forLanguageTag("en-IN"));

    /** Regenerates lead.aiSummary and lead.nextBestAction in place from the
     *  lead's current state. Call this AFTER LeadScoringService.rescore() so
     *  the insight reflects the freshly computed score/intent, not stale ones. */
    public void generate(Lead lead) {
        lead.setAiSummary(buildSummary(lead));
        lead.setNextBestAction(buildNextBestAction(lead));
        lead.setAiSummaryGeneratedAt(Instant.now());
    }

    private String buildSummary(Lead lead) {
        StringBuilder sb = new StringBuilder();
        long daysSince = lead.getDaysSinceLastTouchpoint();
        String name = lead.getFullName();

        sb.append(name).append(" is at the ").append(titleCase(lead.getStage().name())).append(" stage");
        if (lead.getInterest() != null && !lead.getInterest().isBlank()) {
            sb.append(", interested in ").append(lead.getInterest());
        }
        sb.append(". ");

        sb.append("Lead score ").append(lead.getScore()).append("/100");
        if (lead.getBuyingIntent() != null) {
            sb.append(" (").append(titleCase(lead.getBuyingIntent().name())).append(" buying intent)");
        }
        sb.append(". ");

        if (daysSince < 0) {
            sb.append("No touchpoint has been logged yet — this lead has never been contacted. ");
        } else if (daysSince == 0) {
            sb.append("Last contacted today. ");
        } else {
            sb.append("Last contacted ").append(daysSince).append(daysSince == 1 ? " day ago. " : " days ago. ");
        }

        if (lead.getBudget() != null && lead.getBudget().signum() > 0) {
            sb.append("Stated budget is ₹").append(lead.getBudget().toBigInteger()).append(". ");
        }

        if (lead.getConvertProbability() != null) {
            sb.append("Estimated ").append(lead.getConvertProbability()).append("% likelihood to convert. ");
        }

        return sb.toString().trim();
    }

    private String buildNextBestAction(Lead lead) {
        LeadStage stage = lead.getStage();
        long daysSince = lead.getDaysSinceLastTouchpoint();

        if (stage == LeadStage.WON || stage == LeadStage.LOST) {
            return null; // Closed leads don't need a next action.
        }

        boolean neverContacted = daysSince < 0;
        boolean overdue = daysSince >= 7;
        boolean dueSoon = daysSince >= 3 && daysSince < 7;
        boolean hot = lead.getBuyingIntent() == BuyingIntent.HOT;
        boolean urgent = lead.getPriority() == Priority.URGENT || lead.getPriority() == Priority.HIGH;

        if (neverContacted) {
            return "Make first contact — this lead has never been touched.";
        }
        if (overdue && hot) {
            return "Call today — high-intent lead has gone " + daysSince + " days without contact.";
        }
        if (overdue) {
            return "Follow up now — inactive for " + daysSince + " days and at risk of going cold.";
        }
        if (stage == LeadStage.NEW) {
            return "Qualify this lead — confirm budget and interest, then move to Contacted.";
        }
        if (stage == LeadStage.CONTACTED && dueSoon) {
            return "Send a follow-up — no response logged in " + daysSince + " days.";
        }
        if (stage == LeadStage.QUALIFIED) {
            return "Schedule a meeting to walk through the requirement in detail.";
        }
        if (stage == LeadStage.MEETING) {
            return "Capture requirements from the meeting, then advance to Opportunity.";
        }
        if (stage == LeadStage.OPPORTUNITY && urgent) {
            return "Push to close — high-priority opportunity, prepare and send a quotation this week.";
        }
        if (stage == LeadStage.OPPORTUNITY) {
            return "Source pricing and prepare a quotation to move this forward.";
        }
        if (stage == LeadStage.QUOTATION) {
            return "Quotation sent — follow up to confirm the customer's decision.";
        }
        if (dueSoon) {
            return "Check in soon — " + daysSince + " days since last contact.";
        }
        return "On track — no action needed right now.";
    }

    private String titleCase(String enumName) {
        String lower = enumName.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
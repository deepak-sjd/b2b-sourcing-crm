package com.doctutorials.crm.lead;

import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Deterministic, explainable scoring - deliberately NOT a black-box model.
 * Production teams need to be able to say "why did this lead get 82".
 * Swap this out for a real ML model later behind the same interface.
 */
@Service
public class LeadScoringService {

    public void rescore(Lead lead) {
        int score = 0;

        // Recency of contact
        long daysSinceContact = lead.getDaysSinceLastTouchpoint();
        if (daysSinceContact >= 0) {
            if (daysSinceContact <= 2) score += 30;
            else if (daysSinceContact <= 7) score += 15;
            else score -= 10;
        }

        // Stage progress
        score += switch (lead.getStage()) {
            case NEW -> 10;
            case CONTACTED -> 25;
            case QUALIFIED -> 45;
            case MEETING -> 55;
            case OPPORTUNITY -> 65;
            case QUOTATION -> 80;
            case WON -> 100;
            case LOST -> 0;
        };

        // Budget signal - has a budget stated at all
        if (lead.getBudget() != null && lead.getBudget().signum() > 0) {
            score += 10;
        }

        // Priority as a manual override signal from the counselor
        score += switch (lead.getPriority()) {
            case URGENT -> 15;
            case HIGH -> 10;
            case MEDIUM -> 5;
            case LOW -> 0;
        };

        lead.setScore(Math.max(0, Math.min(100, score)));
        lead.setBuyingIntent(deriveIntent(lead.getScore()));
        lead.setConvertProbability(estimateConvertProbability(lead));
    }

    private BuyingIntent deriveIntent(int score) {
        if (score >= 75) return BuyingIntent.HOT;
        if (score >= 50) return BuyingIntent.WARM;
        if (score >= 25) return BuyingIntent.COOL;
        return BuyingIntent.COLD;
    }

    private int estimateConvertProbability(Lead lead) {
        // Simple heuristic blend; replace with a trained model's output later.
        int base = lead.getScore() / 2;
        if (lead.getStage() == LeadStage.QUALIFIED) base += 8;
        if (lead.getStage() == LeadStage.MEETING) base += 12;
        if (lead.getStage() == LeadStage.OPPORTUNITY) base += 15;
        if (lead.getStage() == LeadStage.QUOTATION) base += 22;
        return Math.max(0, Math.min(100, base));
    }

    public boolean isStale(Lead lead, long thresholdDays) {
        return lead.getLastTouchpointAt() != null
                && lead.getDaysSinceLastTouchpoint() >= thresholdDays
                && lead.getStage() != LeadStage.WON
                && lead.getStage() != LeadStage.LOST;
    }

    public void markTouched(Lead lead) {
        lead.setLastTouchpointAt(Instant.now());
        rescore(lead);
    }
}
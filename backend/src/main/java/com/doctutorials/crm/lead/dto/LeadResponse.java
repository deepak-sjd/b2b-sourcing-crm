package com.doctutorials.crm.lead.dto;

import com.doctutorials.crm.lead.BuyingIntent;
import com.doctutorials.crm.lead.Lead;
import com.doctutorials.crm.lead.LeadStage;
import com.doctutorials.crm.lead.LostReason;
import com.doctutorials.crm.lead.Priority;
import com.doctutorials.crm.lead.ScrapReason;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LeadResponse(
        UUID id,
        String fullName,
        String phone,
        String email,
        String city,
        String interest,
        BigDecimal budget,
        BigDecimal estimatedValue,
        LeadStage stage,
        Priority priority,
        int score,
        BuyingIntent buyingIntent,
        Integer convertProbability,
        String source,
        String ownerName,
        Instant lastTouchpointAt,
        Instant nextFollowUpAt,
        long daysSinceLastTouchpoint,
        boolean atRisk,
        String aiSummary,
        String nextBestAction,
        Instant aiSummaryGeneratedAt,
        // Won/Lost closure (PRD §14.1/§14.2)
        Instant closedAt,
        BigDecimal finalValue,
        String closureRemarks,
        LostReason lostReason,
        String lostComment,
        // Scrap lifecycle (PRD §14.3)
        boolean scrapped,
        ScrapReason scrapReason,
        String scrapRemarks,
        Instant scrappedAt
) {
    public static LeadResponse from(Lead l, long staleThresholdDays) {
        long daysSince = l.getDaysSinceLastTouchpoint();
        boolean atRisk = daysSince >= staleThresholdDays;

        return new LeadResponse(
                l.getId(), l.getFullName(), l.getPhone(), l.getEmail(), l.getCity(),
                l.getInterest(), l.getBudget(), l.getEstimatedValue(),
                l.getStage(), l.getPriority(), l.getScore(), l.getBuyingIntent(),
                l.getConvertProbability(), l.getSource(),
                l.getOwner() != null ? l.getOwner().getFullName() : null,
                l.getLastTouchpointAt(), l.getNextFollowUpAt(), daysSince, atRisk, l.getAiSummary(),
                l.getNextBestAction(), l.getAiSummaryGeneratedAt(),
                l.getClosedAt(), l.getFinalValue(), l.getClosureRemarks(), l.getLostReason(), l.getLostComment(),
                l.isScrapped(), l.getScrapReason(), l.getScrapRemarks(), l.getScrappedAt()
        );
    }
}

package com.doctutorials.crm.contact;

/**
 * Auto-derived engagement state, recomputed by ContactEngagementService
 * whenever a contact is created, edited, or touched. Deliberately explainable
 * rather than a black-box label — see ContactEngagementService for the rules
 * that produce it (same philosophy as lead.BuyingIntent/LeadScoringService).
 */
public enum ContactEngagement {
    NEW,      // added, no activity logged yet
    ACTIVE,   // touched recently
    COOLING,  // touched a while ago, not yet at-risk
    AT_RISK   // stale for too long, or the email on file bounced
}

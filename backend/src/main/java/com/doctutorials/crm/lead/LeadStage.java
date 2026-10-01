package com.doctutorials.crm.lead;

/** Ordered pipeline stages. Order matters for funnel/conversion reports.
 *  Generic sales terminology - not tied to any one business's vocabulary
 *  (a coaching institute might call WON "Enrolled", a reseller might call it
 *  "Order Closed" - that's a display-layer label mapping, not a new enum value).
 *
 *  MEETING and QUOTATION sit between QUALIFIED and WON/LOST, matching the
 *  PRD's funnel (New > Contacted > Qualified > Meeting > Opportunity >
 *  Quotation > Won/Lost). Stored as plain VARCHAR(30) (see V1 migration),
 *  not a native DB enum, so adding these needed no schema migration. */
public enum LeadStage {
    NEW,
    CONTACTED,
    QUALIFIED,
    MEETING,
    OPPORTUNITY,
    QUOTATION,
    WON,
    LOST
}
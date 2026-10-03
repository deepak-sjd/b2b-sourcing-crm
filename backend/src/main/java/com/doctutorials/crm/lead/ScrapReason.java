package com.doctutorials.crm.lead;

/** Categories drawn from the PRD's §14.3 description of what belongs in
 *  Scrap ("not suitable, do not match the requirement, are inactive,
 *  duplicate or incorrect, or are no longer worked on"). The PRD doesn't
 *  give this a strict enum the way it does for LostReason, but a fixed
 *  vocabulary here pays for itself the same way ApprovalStatus does for
 *  Customer: it's filterable/reportable, and scrapRemarks still carries
 *  whatever free-text detail a fixed category can't. */
public enum ScrapReason {
    NOT_SUITABLE,
    REQUIREMENT_MISMATCH,
    INACTIVE,
    DUPLICATE,
    INCORRECT_DATA,
    NO_LONGER_WORKING_ON,
    OTHER
}

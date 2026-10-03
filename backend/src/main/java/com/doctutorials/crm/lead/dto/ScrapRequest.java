package com.doctutorials.crm.lead.dto;

import com.doctutorials.crm.lead.ScrapReason;
import jakarta.validation.constraints.NotNull;

/** scrapReason is @NotNull — PRD §14.3 lists "reason for moving to Scrap" as
 *  information that's always kept on the record. */
public record ScrapRequest(
        @NotNull ScrapReason scrapReason,
        String remarks
) {}

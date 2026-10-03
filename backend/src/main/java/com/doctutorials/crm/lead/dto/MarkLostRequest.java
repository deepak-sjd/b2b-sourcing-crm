package com.doctutorials.crm.lead.dto;

import com.doctutorials.crm.lead.LostReason;
import jakarta.validation.constraints.NotNull;

/** lostReason is @NotNull, not optional — PRD §14.2: "A reason is required,
 *  and the user can add a comment." Enforced here (not just client-side) so
 *  the constraint holds regardless of caller. */
public record MarkLostRequest(
        @NotNull LostReason lostReason,
        String comment
) {}

package com.doctutorials.crm.lead;

import com.doctutorials.crm.common.exception.BadRequestException;

import java.math.BigDecimal;
import java.util.List;

/** The lead funnel rules (PRD §10), in one place so they can be tested and later made configurable. */
public final class LeadStageRules {

    /** Open stages in funnel order. WON and LOST are closures, handled separately. */
    private static final List<LeadStage> FUNNEL = List.of(
            LeadStage.NEW, LeadStage.CONTACTED, LeadStage.QUALIFIED,
            LeadStage.MEETING, LeadStage.OPPORTUNITY, LeadStage.QUOTATION);

    private LeadStageRules() {
    }

    public static void validateMove(LeadStage from, LeadStage to, boolean contactable,
                                    boolean hasRequirement, boolean fromQuotationFlow) {
        if (from == LeadStage.WON || from == LeadStage.LOST) {
            throw bad("Cannot move a lead out of a closed stage (" + from + ")");
        }
        if (to == LeadStage.WON || to == LeadStage.LOST) {
            throw bad("Use the Mark Won / Mark Lost action to close a lead - " + to + " requires closure details");
        }
        if (from == to) {
            throw bad("The lead is already in stage " + from);
        }
        if (fromQuotationFlow) {
            return; // sending a quotation is what moves a lead to QUOTATION
        }
        if (to == LeadStage.QUOTATION) {
            throw bad("A lead moves to QUOTATION automatically when a quotation is sent");
        }
        if (from == LeadStage.QUOTATION) {
            throw bad("A lead with a sent quotation can only be closed as Won or Lost");
        }

        int f = FUNNEL.indexOf(from);
        int t = FUNNEL.indexOf(to);
        if (t < f - 1) {
            throw bad("A lead can only move back one stage at a time: from " + from
                    + " you can go back to " + FUNNEL.get(f - 1));
        }
        if (t >= FUNNEL.indexOf(LeadStage.QUALIFIED) && !contactable) {
            throw bad("Add a phone number or email before moving a lead to " + to);
        }
        if (to == LeadStage.OPPORTUNITY && !hasRequirement) {
            throw bad("Record at least one customer requirement before moving to OPPORTUNITY");
        }
    }

    public static void requireFinalValue(BigDecimal finalValue) {
        if (finalValue == null || finalValue.signum() <= 0) {
            throw bad("Enter the final deal value (greater than zero) to mark a lead as Won");
        }
    }

    private static BadRequestException bad(String message) {
        return new BadRequestException(message);
    }
}
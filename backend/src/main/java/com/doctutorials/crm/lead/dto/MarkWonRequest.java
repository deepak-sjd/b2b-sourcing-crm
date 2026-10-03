package com.doctutorials.crm.lead.dto;

import java.math.BigDecimal;

/** Both fields optional — unlike Lost, the PRD doesn't require a value or
 *  remark to close a lead as Won, only that closedAt gets recorded. */
public record MarkWonRequest(
        BigDecimal finalValue,
        String remarks
) {}

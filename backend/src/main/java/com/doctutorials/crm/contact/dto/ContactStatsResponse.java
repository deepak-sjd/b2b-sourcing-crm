package com.doctutorials.crm.contact.dto;

public record ContactStatsResponse(
        long total,
        long active,
        long cooling,
        long atRisk,
        long bounced,
        long unassigned
) {}

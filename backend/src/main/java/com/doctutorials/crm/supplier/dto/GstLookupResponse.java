package com.doctutorials.crm.supplier.dto;

public record GstLookupResponse(
        boolean structureValid,
        boolean checksumValid,
        String stateCode,
        String stateName,
        String panNumber
) {}

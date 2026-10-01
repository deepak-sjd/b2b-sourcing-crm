package com.doctutorials.crm.supplier.dto;

import java.util.UUID;

public record DuplicateCheckRequest(
        String companyName,
        String gstNumber,
        String contactPhone,
        UUID excludeId
) {}

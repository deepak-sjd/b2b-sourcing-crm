package com.doctutorials.crm.contact.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record ContactRequest(
        @NotBlank String fullName,
        String designation,
        String company,
        String phone,
        String email,
        String city,
        String source,
        UUID ownerId,
        boolean emailBounced
) {}

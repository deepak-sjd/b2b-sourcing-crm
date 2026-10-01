package com.doctutorials.crm.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CustomerContactRequest(
        /** Present on update of an existing contact row; null for a new one — see
         *  CustomerService.syncContacts for how this decides update vs. insert. */
        UUID id,
        @NotBlank String contactType,
        @NotBlank String name,
        @Email String email,
        String contactNo,
        String designation
) {}

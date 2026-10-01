package com.doctutorials.crm.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Shared request/response shape for Customer.billingAddress and shippingAddress. */
public record AddressDto(
        @NotBlank String line1,
        String line2,
        String area,
        String country,
        String state,
        String city,
        String pinCode,
        String phone,
        @Email String email,
        String gstNo
) {}

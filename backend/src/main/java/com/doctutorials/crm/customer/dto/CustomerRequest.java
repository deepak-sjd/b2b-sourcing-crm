package com.doctutorials.crm.customer.dto;

import com.doctutorials.crm.customer.ApprovalStatus;
import com.doctutorials.crm.customer.NatureOfBusiness;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CustomerRequest(
        @NotBlank String companyName,
        String gstNo,
        NatureOfBusiness natureOfBusiness,
        UUID accountManagerId,
        @NotNull @Valid AddressDto billingAddress,
        /** Ignored server-side when shippingSameAsBilling is true (billingAddress
         *  is copied over instead) — see CustomerService.applyRequest. */
        @Valid AddressDto shippingAddress,
        boolean shippingSameAsBilling,
        ApprovalStatus approvalStatus,
        @Valid List<CustomerContactRequest> contacts
) {}

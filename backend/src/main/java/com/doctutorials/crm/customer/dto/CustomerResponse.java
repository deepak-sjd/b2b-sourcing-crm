package com.doctutorials.crm.customer.dto;

import com.doctutorials.crm.customer.Address;
import com.doctutorials.crm.customer.ApprovalStatus;
import com.doctutorials.crm.customer.Customer;
import com.doctutorials.crm.customer.NatureOfBusiness;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String companyName,
        String gstNo,
        NatureOfBusiness natureOfBusiness,
        UUID accountManagerId,
        String accountManagerName,
        AddressDto billingAddress,
        AddressDto shippingAddress,
        boolean shippingSameAsBilling,
        ApprovalStatus approvalStatus,
        boolean active,
        UUID convertedFromLeadId,
        String aiHealthSummary,
        Instant aiHealthGeneratedAt,
        List<CustomerContactResponse> contacts,
        Instant createdAt
) {
    public static CustomerResponse from(Customer c) {
        return new CustomerResponse(
                c.getId(),
                c.getCompanyName(),
                c.getGstNo(),
                c.getNatureOfBusiness(),
                c.getAccountManager() != null ? c.getAccountManager().getId() : null,
                c.getAccountManager() != null ? c.getAccountManager().getFullName() : null,
                toDto(c.getBillingAddress()),
                toDto(c.getShippingAddress()),
                c.isShippingSameAsBilling(),
                c.getApprovalStatus(),
                c.isActive(),
                c.getConvertedFromLeadId(),
                c.getAiHealthSummary(),
                c.getAiHealthGeneratedAt(),
                c.getContacts().stream().map(CustomerContactResponse::from).toList(),
                c.getCreatedAt()
        );
    }

    private static AddressDto toDto(Address a) {
        if (a == null) return null;
        return new AddressDto(
                a.getLine1(), a.getLine2(), a.getArea(), a.getCountry(), a.getState(),
                a.getCity(), a.getPinCode(), a.getPhone(), a.getEmail(), a.getGstNo());
    }
}

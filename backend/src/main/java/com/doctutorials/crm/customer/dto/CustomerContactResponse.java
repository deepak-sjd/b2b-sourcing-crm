package com.doctutorials.crm.customer.dto;

import com.doctutorials.crm.customer.CustomerContact;

import java.util.UUID;

public record CustomerContactResponse(
        UUID id,
        String contactType,
        String name,
        String email,
        String contactNo,
        String designation
) {
    public static CustomerContactResponse from(CustomerContact c) {
        return new CustomerContactResponse(
                c.getId(), c.getContactType(), c.getName(), c.getEmail(), c.getContactNo(), c.getDesignation());
    }
}

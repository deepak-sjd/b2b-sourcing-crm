package com.doctutorials.crm.customer;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

/**
 * Shared shape for both Customer.billingAddress and Customer.shippingAddress.
 * Column names are re-mapped per-embed via @AttributeOverrides on Customer
 * (billing_* / shipping_*) so one class serves both without a name clash.
 */
@Embeddable
@Getter
@Setter
public class Address {

    private String line1;
    private String line2;
    private String area;
    private String country;
    private String state;
    private String city;
    private String pinCode;
    private String phone;
    private String email;

    /** Matches the client's screenshot, which showed a "Billing GST No" field
     *  distinct from the company's main GST number (see Customer.gstNo). */
    private String gstNo;
}

package com.doctutorials.crm.customer;

import com.doctutorials.crm.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "customer_contacts", indexes = {
        @Index(name = "idx_customer_contacts_customer", columnList = "customer_id")
})
@Getter
@Setter
public class CustomerContact extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    /** Free text ("Primary", "Billing", "Technical", ...) rather than an enum —
     *  same reasoning as Lead.interest: different clients label these
     *  differently, and it isn't used for filtering/reporting the way
     *  ApprovalStatus or NatureOfBusiness are. The frontend offers a handful
     *  of suggested values but doesn't lock the field to them. */
    @NotBlank
    private String contactType;

    @NotBlank
    private String name;

    @Email
    private String email;

    private String contactNo;

    private String designation;
}

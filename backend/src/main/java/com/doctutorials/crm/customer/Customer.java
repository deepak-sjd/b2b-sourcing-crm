package com.doctutorials.crm.customer;

import com.doctutorials.crm.common.BaseEntity;
import com.doctutorials.crm.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "customers", indexes = {
        @Index(name = "idx_customers_account_manager", columnList = "account_manager_id"),
        @Index(name = "idx_customers_approval_status", columnList = "approvalStatus"),
        @Index(name = "idx_customers_nature_of_business", columnList = "natureOfBusiness")
})
@Getter
@Setter
public class Customer extends BaseEntity {

    @NotBlank
    @Column(nullable = false)
    private String companyName;

    /** Company-level GSTIN. The billing address also carries its own gstNo field
     *  (see Address) to match the client's screenshot, which showed a separate
     *  "Billing GST No" under the address block — most customers will just set
     *  this one and leave that one blank. */
    private String gstNo;

    @Enumerated(EnumType.STRING)
    private NatureOfBusiness natureOfBusiness;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_manager_id")
    private User accountManager;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "line1", column = @Column(name = "billing_line1")),
            @AttributeOverride(name = "line2", column = @Column(name = "billing_line2")),
            @AttributeOverride(name = "area", column = @Column(name = "billing_area")),
            @AttributeOverride(name = "country", column = @Column(name = "billing_country")),
            @AttributeOverride(name = "state", column = @Column(name = "billing_state")),
            @AttributeOverride(name = "city", column = @Column(name = "billing_city")),
            @AttributeOverride(name = "pinCode", column = @Column(name = "billing_pin_code")),
            @AttributeOverride(name = "phone", column = @Column(name = "billing_phone")),
            @AttributeOverride(name = "email", column = @Column(name = "billing_email")),
            @AttributeOverride(name = "gstNo", column = @Column(name = "billing_gst_no")),
    })
    private Address billingAddress = new Address();

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "line1", column = @Column(name = "shipping_line1")),
            @AttributeOverride(name = "line2", column = @Column(name = "shipping_line2")),
            @AttributeOverride(name = "area", column = @Column(name = "shipping_area")),
            @AttributeOverride(name = "country", column = @Column(name = "shipping_country")),
            @AttributeOverride(name = "state", column = @Column(name = "shipping_state")),
            @AttributeOverride(name = "city", column = @Column(name = "shipping_city")),
            @AttributeOverride(name = "pinCode", column = @Column(name = "shipping_pin_code")),
            @AttributeOverride(name = "phone", column = @Column(name = "shipping_phone")),
            @AttributeOverride(name = "email", column = @Column(name = "shipping_email")),
            @AttributeOverride(name = "gstNo", column = @Column(name = "shipping_gst_no")),
    })
    private Address shippingAddress = new Address();

    /** True when Ship To is the same as Bill To — the UI hides the shipping
     *  section entirely in that case rather than keeping a second, always-in-sync
     *  copy visible. CustomerService still writes shippingAddress = billingAddress
     *  on save when this is true, so reads never have to branch on this flag. */
    @Column(nullable = false)
    private boolean shippingSameAsBilling = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CustomerContact> contacts = new ArrayList<>();

    /** Set when this customer originated from a Lead marked WON. Nullable, and
     *  deliberately NOT a @JoinColumn/@ManyToOne to Lead: Lead and Customer are
     *  independent aggregates today, so archiving or deleting a Lead must never
     *  cascade into this table. Nothing currently populates this field — it's
     *  here so a future "Convert to Customer" action on LeadDetailPage has
     *  somewhere to record the link without a schema change. */
    private UUID convertedFromLeadId;

    /** Rule-based health note from CustomerInsightService — see its class
     *  comment for why this is deterministic rather than LLM-generated. */
    @Column(length = 2000)
    private String aiHealthSummary;
    private Instant aiHealthGeneratedAt;
}

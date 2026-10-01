package com.doctutorials.crm.contact;

import com.doctutorials.crm.common.BaseEntity;
import com.doctutorials.crm.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "contacts", indexes = {
        @Index(name = "idx_contacts_owner", columnList = "owner_id"),
        @Index(name = "idx_contacts_engagement", columnList = "engagement"),
        @Index(name = "idx_contacts_email", columnList = "email"),
        @Index(name = "idx_contacts_phone", columnList = "phone")
})
@Getter
@Setter
public class Contact extends BaseEntity {

    /** The person's name — e.g. "Mukesh", the contact at a customer/account. */
    @NotBlank
    private String fullName;

    private String designation;

    /** The account/company this person belongs to — e.g. "Telivus Systems". */
    private String company;

    private String phone;
    private String email;
    private String city;

    /** e.g. "Referral", "Website", "Meta Ads" — same free-text convention as Lead.source. */
    private String source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    /** Who added this contact. Set once on create, never changed on update. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    /** Last time any touchpoint (call, WhatsApp click, email, manual log) was recorded. */
    private Instant lastActivityAt;

    private Instant nextFollowUpAt;

    /** True once an email to this address is known to have bounced — surfaced
     *  as the "Bounced" filter and forces the engagement state to AT_RISK. */
    @Column(nullable = false)
    private boolean emailBounced = false;

    /** 0-100, recomputed by ContactEngagementService. Same explainable-scoring
     *  philosophy as Lead.score — not a black-box model. */
    @Column(nullable = false)
    private int engagementScore = 20;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContactEngagement engagement = ContactEngagement.NEW;

    /** Short, auto-generated one-liner surfaced in the UI, e.g.
     *  "No activity in 21 days — this contact is going cold." Regenerated
     *  alongside the score, not user-editable. */
    @Column(length = 500)
    private String aiInsight;

    private boolean archived = false;

    @Transient
    public long getDaysSinceLastActivity() {
        if (lastActivityAt == null) return -1;
        return java.time.Duration.between(lastActivityAt, Instant.now()).toDays();
    }
}

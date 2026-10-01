package com.doctutorials.crm.contact.dto;

import com.doctutorials.crm.contact.Contact;
import com.doctutorials.crm.contact.ContactEngagement;

import java.time.Instant;
import java.util.UUID;

public record ContactResponse(
        UUID id,
        String fullName,
        String designation,
        String company,
        String phone,
        String email,
        String city,
        String source,
        UUID ownerId,
        String ownerName,
        String createdByName,
        Instant lastActivityAt,
        Instant nextFollowUpAt,
        long daysSinceLastActivity,
        boolean emailBounced,
        int engagementScore,
        ContactEngagement engagement,
        String aiInsight,
        Instant createdAt
) {
    public static ContactResponse from(Contact c) {
        return new ContactResponse(
                c.getId(),
                c.getFullName(),
                c.getDesignation(),
                c.getCompany(),
                c.getPhone(),
                c.getEmail(),
                c.getCity(),
                c.getSource(),
                c.getOwner() != null ? c.getOwner().getId() : null,
                c.getOwner() != null ? c.getOwner().getFullName() : null,
                c.getCreatedBy() != null ? c.getCreatedBy().getFullName() : null,
                c.getLastActivityAt(),
                c.getNextFollowUpAt(),
                c.getDaysSinceLastActivity(),
                c.isEmailBounced(),
                c.getEngagementScore(),
                c.getEngagement(),
                c.getAiInsight(),
                c.getCreatedAt()
        );
    }
}

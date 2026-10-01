package com.doctutorials.crm.organization;

import com.doctutorials.crm.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * The tenant boundary: one client business using this CRM. Every Team, and
 * every User, belongs to exactly one Organization.
 *
 * Deliberately minimal right now - just enough to attach data to. What
 * "multi-tenant" actually MEANS operationally (one shared database with
 * every query filtered by organization, vs. a separate database per
 * client) is still an open decision with the client, and this entity
 * doesn't answer that question - it just makes sure the data model isn't
 * blocked on the answer. No query in the app filters by organization yet
 * (LeadAccessService/TaskAccessService/CallAccessService are still
 * team-scoped only) - that's the next real decision, not assumed here.
 */
@Entity
@Table(name = "organizations")
@Getter
@Setter
public class Organization extends BaseEntity {

    @NotBlank
    @Column(nullable = false)
    private String name;

    /** URL-safe unique identifier (e.g. for a future subdomain-per-client scheme). */
    @NotBlank
    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private boolean active = true;
}

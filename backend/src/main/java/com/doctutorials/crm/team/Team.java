package com.doctutorials.crm.team;

import com.doctutorials.crm.organization.Organization;
import com.doctutorials.crm.tenant.TenantEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "teams")
@Getter
@Setter
public class Team extends TenantEntity {

    @NotBlank
    private String name;

    private String description;

    // Read-only: the tenant column is written by Hibernate via TenantEntity.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", insertable = false, updatable = false)
    private Organization organization;
}
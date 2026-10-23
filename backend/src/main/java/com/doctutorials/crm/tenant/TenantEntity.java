package com.doctutorials.crm.tenant;

import com.doctutorials.crm.common.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.TenantId;

import java.util.UUID;

/**
 * Base class for every tenant-owned business record. Hibernate adds the tenant
 * condition to queries on these entities and fills organization_id on insert.
 * There is deliberately no setter: application code must never choose the tenant.
 * Native SQL is NOT filtered automatically; add the tenant condition by hand there.
 */
@Getter
@MappedSuperclass
public abstract class TenantEntity extends BaseEntity {

    @TenantId
    @JsonIgnore
    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;
}
package com.doctutorials.crm.audit;

import com.doctutorials.crm.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_entity", columnList = "entity_type, entity_id"),
        @Index(name = "idx_audit_logs_created_at", columnList = "createdAt")
})
@Getter
@Setter
public class AuditLog extends BaseEntity {

    private UUID actorUserId;
    private String actorName;

    private String entityType;   // e.g. "Lead", "Task"
    private UUID entityId;

    @Enumerated(EnumType.STRING)
    private AuditAction action;

    private String description; // human-readable summary, e.g. "Stage changed from NEW to CONTACTED"
}

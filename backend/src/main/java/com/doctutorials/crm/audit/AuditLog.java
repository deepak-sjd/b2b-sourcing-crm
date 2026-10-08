package com.doctutorials.crm.audit;

import com.doctutorials.crm.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_entity", columnList = "entity_type, entity_id"),
        @Index(name = "idx_audit_logs_created_at", columnList = "createdAt"),
        @Index(name = "idx_audit_logs_entity_created_at", columnList = "entity_type, entity_id, createdAt")
})
@Getter
@Setter
public class AuditLog extends BaseEntity {

    private UUID actorUserId;
    private String actorName;

    private String entityType;
    private UUID entityId;

    @Enumerated(EnumType.STRING)
    private AuditAction action;

    private String description;

    // Stored as JSON text (Map<String,String>), not a typed object - see
    // DELIVERABLE assumption #4. Null for every pre-V50 row and for log()
    // (only logChange() populates these).
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String oldValues;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String newValues;

    private String changedFields; // comma-joined field names, matches migration VARCHAR(500)

    private String ipAddress;
}
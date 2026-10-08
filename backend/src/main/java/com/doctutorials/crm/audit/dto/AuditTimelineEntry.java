package com.doctutorials.crm.audit.dto;

import com.doctutorials.crm.audit.AuditAction;
import com.doctutorials.crm.audit.AuditLog;

import java.time.Instant;
import java.util.UUID;

// Response shape per spec item 5 - `at` instead of `createdAt` on purpose.
public record AuditTimelineEntry(
        UUID id, Instant at, String actorName, String entityType, UUID entityId,
        AuditAction action, String description, String changedFields, String oldValues, String newValues) {

    public static AuditTimelineEntry from(AuditLog log) {
        return new AuditTimelineEntry(log.getId(), log.getCreatedAt(), log.getActorName(), log.getEntityType(),
                log.getEntityId(), log.getAction(), log.getDescription(), log.getChangedFields(),
                log.getOldValues(), log.getNewValues());
    }
}
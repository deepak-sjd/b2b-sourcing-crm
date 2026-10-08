package com.doctutorials.crm.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, UUID entityId, Pageable pageable);

    Page<AuditLog> findByEntityType(String entityType, Pageable pageable);

    @Query(value = """
        SELECT * FROM audit_logs a
        WHERE (:entityType IS NULL OR a.entity_type = :entityType)
          AND (:actorUserId IS NULL OR a.actor_user_id = :actorUserId)
          AND (:action IS NULL OR a.action = :action)
          AND (:from IS NULL OR a.created_at >= :from)
          AND (:to IS NULL OR a.created_at <= :to)
        ORDER BY a.created_at DESC
        """,
        countQuery = """
        SELECT count(*) FROM audit_logs a
        WHERE (:entityType IS NULL OR a.entity_type = :entityType)
          AND (:actorUserId IS NULL OR a.actor_user_id = :actorUserId)
          AND (:action IS NULL OR a.action = :action)
          AND (:from IS NULL OR a.created_at >= :from)
          AND (:to IS NULL OR a.created_at <= :to)
        """,
        nativeQuery = true)
    Page<AuditLog> search(@Param("entityType") String entityType, @Param("action") AuditAction action,
            @Param("actorUserId") UUID actorUserId, @Param("from") Instant from, @Param("to") Instant to,
            Pageable pageable);

    /**
     * Merged, date-ordered journey for a lead = audit_logs on the lead itself
     * plus audit_logs on every child record (requirement, supplier enquiry,
     * activity, task, note, document, quotation) that belongs to it.
     * Quotations are bridged via customers.converted_from_lead_id — see
     * DELIVERABLE assumption #1.
     */
    @Query(value = """
        SELECT * FROM audit_logs a
        WHERE (
            (a.entity_type = 'Lead' AND a.entity_id = :leadId)
            OR (a.entity_type = 'Requirement' AND a.entity_id IN (SELECT id FROM requirements WHERE lead_id = :leadId))
            OR (a.entity_type = 'SupplierEnquiry' AND a.entity_id IN (SELECT id FROM supplier_enquiries WHERE lead_id = :leadId))
            OR (a.entity_type = 'Activity' AND a.entity_id IN (SELECT id FROM activities WHERE lead_id = :leadId))
            OR (a.entity_type = 'Task' AND a.entity_id IN (SELECT id FROM tasks WHERE lead_id = :leadId))
            OR (a.entity_type = 'Note' AND a.entity_id IN (SELECT id FROM notes WHERE lead_id = :leadId))
            OR (a.entity_type = 'Document' AND a.entity_id IN (SELECT id FROM documents WHERE lead_id = :leadId))
            OR (a.entity_type = 'Quotation' AND a.entity_id IN (
                SELECT q.id FROM quotations q JOIN customers c ON c.id = q.customer_id
                WHERE c.converted_from_lead_id = :leadId))
        )
        AND (:entityType IS NULL OR a.entity_type = :entityType)
        AND (:from IS NULL OR a.created_at >= :from)
        AND (:to IS NULL OR a.created_at <= :to)
        ORDER BY a.created_at DESC
        """,
        countQuery = """
        SELECT count(*) FROM audit_logs a
        WHERE (
            (a.entity_type = 'Lead' AND a.entity_id = :leadId)
            OR (a.entity_type = 'Requirement' AND a.entity_id IN (SELECT id FROM requirements WHERE lead_id = :leadId))
            OR (a.entity_type = 'SupplierEnquiry' AND a.entity_id IN (SELECT id FROM supplier_enquiries WHERE lead_id = :leadId))
            OR (a.entity_type = 'Activity' AND a.entity_id IN (SELECT id FROM activities WHERE lead_id = :leadId))
            OR (a.entity_type = 'Task' AND a.entity_id IN (SELECT id FROM tasks WHERE lead_id = :leadId))
            OR (a.entity_type = 'Note' AND a.entity_id IN (SELECT id FROM notes WHERE lead_id = :leadId))
            OR (a.entity_type = 'Document' AND a.entity_id IN (SELECT id FROM documents WHERE lead_id = :leadId))
            OR (a.entity_type = 'Quotation' AND a.entity_id IN (
                SELECT q.id FROM quotations q JOIN customers c ON c.id = q.customer_id
                WHERE c.converted_from_lead_id = :leadId))
        )
        AND (:entityType IS NULL OR a.entity_type = :entityType)
        AND (:from IS NULL OR a.created_at >= :from)
        AND (:to IS NULL OR a.created_at <= :to)
        """,
        nativeQuery = true)
    Page<AuditLog> findLeadTimeline(@Param("leadId") UUID leadId, @Param("entityType") String entityType,
            @Param("from") Instant from, @Param("to") Instant to, Pageable pageable);
}
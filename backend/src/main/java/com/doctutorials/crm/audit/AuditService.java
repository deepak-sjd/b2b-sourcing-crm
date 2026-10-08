package com.doctutorials.crm.audit;

import com.doctutorials.crm.user.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Fire-and-forget audit writer. log() is unchanged - every existing caller
 * keeps working as-is. logChange() is additive: diffs before/after, masks
 * sensitive fields, and is what the new timeline reads to show "Previous ->
 * Updated".
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    private static final int MAX_VALUE_LENGTH = 500;

    // Substring match on the map key, case-insensitive - catches
    // accountNumber, bankAccountNumber, passwordHash, apiToken, etc. without
    // needing every caller to remember to mask.
    private static final List<String> SENSITIVE_KEY_FRAGMENTS = List.of(
            "password", "token", "secret", "accountnumber", "bankaccount",
            "cardnumber", "cvv", "ssn", "apikey", "ifsccode");

    public void log(String entityType, UUID entityId, AuditAction action, String description) {
        AuditLog entry = new AuditLog();
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setAction(action);
        entry.setDescription(description);
        applyActor(entry);
        auditLogRepository.save(entry);
    }

    /**
     * Diffs before/after and stores only the fields that actually changed.
     * before/after should hold the same key set describing "state at that
     * moment" - a key present in only one side counts as changed too (e.g.
     * assigning an owner for the first time).
     */
    public void logChange(String entityType, UUID entityId, AuditAction action, String description,
            Map<String, Object> before, Map<String, Object> after) {
        Map<String, Object> changedBefore = new LinkedHashMap<>();
        Map<String, Object> changedAfter = new LinkedHashMap<>();
        List<String> changedFields = new ArrayList<>();

        Set<String> keys = new LinkedHashSet<>();
        if (before != null) keys.addAll(before.keySet());
        if (after != null) keys.addAll(after.keySet());

        for (String key : keys) {
            Object beforeVal = before != null ? before.get(key) : null;
            Object afterVal = after != null ? after.get(key) : null;
            if (!Objects.equals(stringify(key, beforeVal), stringify(key, afterVal))) {
                changedFields.add(key);
                changedBefore.put(key, stringify(key, beforeVal));
                changedAfter.put(key, stringify(key, afterVal));
            }
        }

        AuditLog entry = new AuditLog();
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setAction(action);
        entry.setDescription(description);
        entry.setChangedFields(String.join(",", changedFields));
        entry.setOldValues(toJson(changedBefore));
        entry.setNewValues(toJson(changedAfter));
        applyActor(entry);
        auditLogRepository.save(entry);
    }

    private String stringify(String key, Object value) {
        if (value == null) return null;
        boolean sensitive = SENSITIVE_KEY_FRAGMENTS.stream()
                .anyMatch(frag -> key.toLowerCase(Locale.ROOT).contains(frag));
        if (sensitive) return "***MASKED***";
        String s = String.valueOf(value);
        return s.length() > MAX_VALUE_LENGTH ? s.substring(0, MAX_VALUE_LENGTH) + "…" : s;
    }

    private String toJson(Map<String, Object> map) {
        if (map.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return null; // never let audit logging break the calling transaction
        }
    }

    private void applyActor(AuditLog entry) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            entry.setActorUserId(user.getId());
            entry.setActorName(user.getFullName());
        }
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> listAll(String entityType, AuditAction action, UUID actorUserId,
            Instant from, Instant to, Pageable pageable) {
        return auditLogRepository.search(entityType, action, actorUserId, from, to, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> listByEntity(String entityType, UUID entityId, Pageable pageable) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable);
    }

    /** Merged journey for one lead - see AuditLogRepository.findLeadTimeline. */
    @Transactional(readOnly = true)
    public Page<AuditLog> leadTimeline(UUID leadId, String entityType, Instant from, Instant to, Pageable pageable) {
        return auditLogRepository.findLeadTimeline(leadId, entityType, from, to, pageable);
    }
}
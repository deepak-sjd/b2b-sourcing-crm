package com.doctutorials.crm.tenant;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Holds "which tenant is this thread working for". Set by JwtAuthFilter per request,
 * and by TenantRunner for scheduled jobs. With nothing set, SYSTEM is used; it matches
 * no real tenant, so tenant-owned queries return nothing (fail closed).
 */
public final class TenantContext {

    public static final UUID SYSTEM = new UUID(0L, 0L);

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(UUID tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId must not be null");
        }
        CURRENT.set(tenantId);
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static UUID currentOrSystem() {
        UUID id = CURRENT.get();
        return id != null ? id : SYSTEM;
    }

    public static <T> T supply(UUID tenantId, Supplier<T> action) {
        UUID previous = CURRENT.get();
        set(tenantId);
        try {
            return action.get();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    public static void runAs(UUID tenantId, Runnable action) {
        supply(tenantId, () -> {
            action.run();
            return null;
        });
    }
}
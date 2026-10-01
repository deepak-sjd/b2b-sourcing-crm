import { apiClient } from "./client";
import type { ApiResponse, PageResponse } from "@/types/api";
import type { AuditLogEntry, AuditLogFilters } from "@/types/auditLog";

export const auditLogsApi = {
  /** Admin-only on the backend (see AuditController) — org-wide trail. */
  async list(filters: AuditLogFilters): Promise<PageResponse<AuditLogEntry>> {
    const { data } = await apiClient.get<ApiResponse<PageResponse<AuditLogEntry>>>("/audit-logs", {
      params: { entityType: filters.entityType, page: filters.page ?? 0, size: filters.size ?? 20 },
    });
    return data.data;
  },

  /** Admins can look up any entity; others only a "Lead" they can already see. */
  async byEntity(entityType: string, entityId: string, page = 0, size = 20): Promise<PageResponse<AuditLogEntry>> {
    const { data } = await apiClient.get<ApiResponse<PageResponse<AuditLogEntry>>>(
      `/audit-logs/by-entity/${entityType}/${entityId}`,
      { params: { page, size } },
    );
    return data.data;
  },
};

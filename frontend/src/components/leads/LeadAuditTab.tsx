import { useQuery } from "@tanstack/react-query";
import { ScrollText } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/common/EmptyState";
import { auditLogsApi } from "@/api/auditLogs";
import { formatDate } from "@/lib/utils";

export function LeadAuditTab({ leadId }: { leadId: string }) {
  const auditQuery = useQuery({
    queryKey: ["audit-logs", "by-entity", "Lead", leadId],
    queryFn: () => auditLogsApi.byEntity("Lead", leadId),
  });

  if (auditQuery.isLoading) {
    return (
      <div className="space-y-2">
        <Skeleton className="h-10 w-full" />
        <Skeleton className="h-10 w-full" />
      </div>
    );
  }

  if (!auditQuery.data || auditQuery.data.content.length === 0) {
    return (
      <EmptyState
        icon={ScrollText}
        title="No audit entries for this lead yet"
        description="Every create, update, and stage change on this lead will show up here."
      />
    );
  }

  return (
    <div className="divide-y divide-border rounded-lg border border-border">
      {auditQuery.data.content.map((entry) => (
        <div key={entry.id} className="flex items-start justify-between gap-3 p-3 text-sm">
          <div>
            <p>{entry.description}</p>
            <p className="mt-0.5 text-xs text-muted-foreground">{entry.actorName ?? "System"}</p>
          </div>
          <p className="shrink-0 text-xs text-muted-foreground">{formatDate(entry.createdAt, true)}</p>
        </div>
      ))}
    </div>
  );
}

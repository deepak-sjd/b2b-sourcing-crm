import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { ScrollText } from "lucide-react";
import { PageHeader } from "@/components/common/PageHeader";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/common/EmptyState";
import { Pagination } from "@/components/common/Pagination";
import { Input } from "@/components/ui/input";
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from "@/components/ui/table";
import { auditLogsApi } from "@/api/auditLogs";
import { formatDate } from "@/lib/utils";

const PAGE_SIZE = 25;

export function AuditLogPage() {
  const [page, setPage] = useState(0);
  const [entityType, setEntityType] = useState("");

  const auditQuery = useQuery({
    queryKey: ["audit-logs", { page, entityType }],
    queryFn: () => auditLogsApi.list({ page, size: PAGE_SIZE, entityType: entityType.trim() || undefined }),
  });

  return (
    <div>
      <PageHeader title="Audit Log" description="A record of who changed what, across the whole CRM." />

      <Card>
        <div className="flex items-center gap-2 border-b border-border p-3">
          <Input
            placeholder="Filter by entity type (e.g. Lead, Task, Call)…"
            className="max-w-xs"
            value={entityType}
            onChange={(e) => {
              setEntityType(e.target.value);
              setPage(0);
            }}
          />
        </div>

        {auditQuery.isLoading ? (
          <div className="space-y-2 p-4">
            {Array.from({ length: 6 }).map((_, i) => (
              <Skeleton key={i} className="h-10 w-full" />
            ))}
          </div>
        ) : auditQuery.data && auditQuery.data.content.length > 0 ? (
          <>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>When</TableHead>
                  <TableHead>Actor</TableHead>
                  <TableHead>Entity</TableHead>
                  <TableHead>Action</TableHead>
                  <TableHead>Description</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {auditQuery.data.content.map((entry) => (
                  <TableRow key={entry.id}>
                    <TableCell className="whitespace-nowrap text-muted-foreground">
                      {formatDate(entry.createdAt, true)}
                    </TableCell>
                    <TableCell>{entry.actorName ?? "System"}</TableCell>
                    <TableCell className="text-muted-foreground">{entry.entityType}</TableCell>
                    <TableCell>
                      <span className="rounded-full bg-muted px-2 py-0.5 text-xs font-medium">
                        {entry.action.replaceAll("_", " ")}
                      </span>
                    </TableCell>
                    <TableCell>{entry.description}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            <Pagination
              page={auditQuery.data.page}
              totalPages={auditQuery.data.totalPages}
              totalElements={auditQuery.data.totalElements}
              onPageChange={setPage}
            />
          </>
        ) : (
          <EmptyState icon={ScrollText} title="No audit entries yet" description="Every create, update, delete, and stage change will show up here." />
        )}
      </Card>
    </div>
  );
}

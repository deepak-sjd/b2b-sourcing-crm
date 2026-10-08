import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { ChevronDown, ChevronRight, ScrollText } from "lucide-react";
import { PageHeader } from "@/components/common/PageHeader";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/common/EmptyState";
import { Pagination } from "@/components/common/Pagination";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { SearchSelect, type SearchSelectOption } from "@/components/common/SearchSelect";
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from "@/components/ui/table";
import { AuditDiffTable } from "@/components/audit/AuditDiffTable";
import { auditLogsApi } from "@/api/auditLogs";
import { usersApi } from "@/api/users";
import { formatDate } from "@/lib/utils";
import { AUDIT_ACTIONS, AUDIT_ENTITY_TYPES, type AuditAction } from "@/types/auditLog";

const PAGE_SIZE = 25;

export function AuditLogPage() {
  const [page, setPage] = useState(0);
  const [entityType, setEntityType] = useState<string>("");
  const [action, setAction] = useState<AuditAction | "">("");
  const [actorUserId, setActorUserId] = useState<string | null>(null);
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [expandedId, setExpandedId] = useState<string | null>(null);

  const usersQuery = useQuery({ queryKey: ["users", "all"], queryFn: usersApi.listAll });
  const userOptions: SearchSelectOption[] = (usersQuery.data ?? []).map((u) => ({ value: u.id, label: u.fullName }));

  const auditQuery = useQuery({
    queryKey: ["audit-logs", { page, entityType, action, actorUserId, from, to }],
    queryFn: () =>
      auditLogsApi.list({
        page, size: PAGE_SIZE,
        entityType: entityType || undefined,
        action: action || undefined,
        actorUserId: actorUserId || undefined,
        from: from ? new Date(from).toISOString() : undefined,
        to: to ? new Date(to).toISOString() : undefined,
      }),
  });

  function resetAndSet<T>(setter: (v: T) => void) {
    return (v: T) => { setter(v); setPage(0); };
  }

  return (
    <div>
      <PageHeader title="Audit Log" description="A record of who changed what, across the whole CRM." />

      <Card>
        <div className="flex flex-wrap items-center gap-2 border-b border-border p-3">
          <Select value={entityType || "ALL"} onValueChange={resetAndSet((v) => setEntityType(v === "ALL" ? "" : v))}>
            <SelectTrigger className="w-40"><SelectValue placeholder="Entity type" /></SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">All entities</SelectItem>
              {AUDIT_ENTITY_TYPES.map((t) => <SelectItem key={t} value={t}>{t}</SelectItem>)}
            </SelectContent>
          </Select>

          <Select value={action || "ALL"} onValueChange={resetAndSet((v) => setAction(v === "ALL" ? "" : (v as AuditAction)))}>
            <SelectTrigger className="w-40"><SelectValue placeholder="Action" /></SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">All actions</SelectItem>
              {AUDIT_ACTIONS.map((a) => <SelectItem key={a} value={a}>{a.replaceAll("_", " ")}</SelectItem>)}
            </SelectContent>
          </Select>

          <SearchSelect
            options={userOptions}
            value={actorUserId}
            onChange={resetAndSet(setActorUserId)}
            placeholder="Filter by user…"
            className="w-48"
          />

          <Input type="date" className="w-36" value={from} onChange={(e) => resetAndSet(setFrom)(e.target.value)} />
          <span className="text-xs text-muted-foreground">to</span>
          <Input type="date" className="w-36" value={to} onChange={(e) => resetAndSet(setTo)(e.target.value)} />
        </div>

        {auditQuery.isLoading ? (
          <div className="space-y-2 p-4">
            {Array.from({ length: 6 }).map((_, i) => <Skeleton key={i} className="h-10 w-full" />)}
          </div>
        ) : auditQuery.data && auditQuery.data.content.length > 0 ? (
          <>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-8" />
                  <TableHead>When</TableHead>
                  <TableHead>Actor</TableHead>
                  <TableHead>Entity</TableHead>
                  <TableHead>Action</TableHead>
                  <TableHead>Description</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {auditQuery.data.content.map((entry) => {
                  const hasDiff = !!entry.changedFields;
                  const expanded = expandedId === entry.id;
                  return (
                    <>
                      <TableRow
                        key={entry.id}
                        className={hasDiff ? "cursor-pointer" : ""}
                        onClick={() => hasDiff && setExpandedId(expanded ? null : entry.id)}
                      >
                        <TableCell>
                          {hasDiff && (expanded ? <ChevronDown className="h-3.5 w-3.5" /> : <ChevronRight className="h-3.5 w-3.5" />)}
                        </TableCell>
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
                      {expanded && (
                        <TableRow key={entry.id + "-diff"}>
                          <TableCell colSpan={6} className="bg-muted/30 p-3">
                            <AuditDiffTable
                              changedFields={entry.changedFields}
                              oldValues={entry.oldValues}
                              newValues={entry.newValues}
                            />
                          </TableCell>
                        </TableRow>
                      )}
                    </>
                  );
                })}
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
import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { ClipboardList, Search } from "lucide-react";
import { PageHeader } from "@/components/common/PageHeader";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/common/EmptyState";
import { Pagination } from "@/components/common/Pagination";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { PriorityBadge } from "@/components/leads/LeadBadges";
import { NeedsSupplierBadge, RequirementStatusBadge } from "@/components/requirements/RequirementBadges";
import { requirementsApi } from "@/api/requirements";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import { formatCurrencyINR, formatDate } from "@/lib/utils";
import { REQUIREMENT_STATUSES, REQUIREMENT_STATUS_LABELS, type RequirementStatus } from "@/types/requirement";

const PAGE_SIZE = 20;
type SupplierFilter = "ALL" | "YES" | "NO";

function SummaryCard({ label, value, loading }: { label: string; value: number; loading: boolean }) {
  return (
    <Card>
      <CardContent className="p-5">
        <p className="text-sm text-muted-foreground">{label}</p>
        {loading ? <Skeleton className="mt-2 h-7 w-12" /> : <p className="mt-1 text-2xl font-semibold">{value}</p>}
      </CardContent>
    </Card>
  );
}

export function RequirementsPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState<RequirementStatus | "ALL">("ALL");
  const [supplier, setSupplier] = useState<SupplierFilter>("ALL");
  const q = useDebouncedValue(search, 300);

  const summaryQuery = useQuery({ queryKey: ["requirements", "summary"], queryFn: requirementsApi.summary });
  const listQuery = useQuery({
    queryKey: ["requirements", "list", { page, q, status, supplier }],
    queryFn: () =>
      requirementsApi.list({
        page,
        size: PAGE_SIZE,
        q,
        status: status === "ALL" ? undefined : status,
        needsSupplier: supplier === "ALL" ? undefined : supplier === "YES",
      }),
  });

  return (
    <div>
      <PageHeader
        title="Requirements"
        description="Everything customers have asked for. Add new ones from a lead's Requirements tab."
      />

      <div className="mb-4 grid grid-cols-2 gap-4 lg:grid-cols-4">
        <SummaryCard label="Open" value={summaryQuery.data?.open ?? 0} loading={summaryQuery.isLoading} />
        <SummaryCard label="Awaiting supplier" value={summaryQuery.data?.awaitingSupplier ?? 0} loading={summaryQuery.isLoading} />
        <SummaryCard label="Sourcing" value={summaryQuery.data?.sourcing ?? 0} loading={summaryQuery.isLoading} />
        <SummaryCard label="Quoted" value={summaryQuery.data?.quoted ?? 0} loading={summaryQuery.isLoading} />
      </div>

      <Card>
        <div className="flex flex-wrap items-center gap-2 border-b border-border p-3">
          <div className="relative min-w-56 flex-1">
            <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
            <Input
              className="pl-8"
              placeholder="Search product or customer…"
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setPage(0);
              }}
            />
          </div>
          <Select
            value={status}
            onValueChange={(v) => {
              setStatus(v as RequirementStatus | "ALL");
              setPage(0);
            }}
          >
            <SelectTrigger className="w-40">
              <SelectValue placeholder="Status" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">All statuses</SelectItem>
              {REQUIREMENT_STATUSES.map((s) => (
                <SelectItem key={s} value={s}>
                  {REQUIREMENT_STATUS_LABELS[s]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Select
            value={supplier}
            onValueChange={(v) => {
              setSupplier(v as SupplierFilter);
              setPage(0);
            }}
          >
            <SelectTrigger className="w-44">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">Supplier: any</SelectItem>
              <SelectItem value="YES">Needs supplier</SelectItem>
              <SelectItem value="NO">In stock / direct</SelectItem>
            </SelectContent>
          </Select>
        </div>

        {listQuery.isLoading ? (
          <div className="space-y-2 p-4">
            {Array.from({ length: 5 }).map((_, i) => (
              <Skeleton key={i} className="h-10 w-full" />
            ))}
          </div>
        ) : listQuery.data && listQuery.data.content.length > 0 ? (
          <>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Product / service</TableHead>
                  <TableHead>Customer</TableHead>
                  <TableHead>Qty</TableHead>
                  <TableHead>Budget</TableHead>
                  <TableHead>Priority</TableHead>
                  <TableHead>Delivery by</TableHead>
                  <TableHead>Assigned</TableHead>
                  <TableHead>Status</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {listQuery.data.content.map((r) => (
                  <TableRow key={r.id}>
                    <TableCell>
                      <p className="font-medium">{r.productName}</p>
                      {r.needsSupplier && <div className="mt-1"><NeedsSupplierBadge /></div>}
                    </TableCell>
                    <TableCell>
                      <Link to={`/leads/${r.leadId}`} className="text-primary hover:underline">
                        {r.leadName}
                      </Link>
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {r.quantity != null ? `${r.quantity}${r.unit ? " " + r.unit : ""}` : "—"}
                    </TableCell>
                    <TableCell className="text-muted-foreground">{formatCurrencyINR(r.budget)}</TableCell>
                    <TableCell>
                      <PriorityBadge priority={r.priority} />
                    </TableCell>
                    <TableCell className="text-muted-foreground">{formatDate(r.requiredDeliveryDate)}</TableCell>
                    <TableCell className="text-muted-foreground">{r.assignedToName ?? "Unassigned"}</TableCell>
                    <TableCell>
                      <RequirementStatusBadge status={r.status} />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            <Pagination
              page={listQuery.data.page}
              totalPages={listQuery.data.totalPages}
              totalElements={listQuery.data.totalElements}
              onPageChange={setPage}
            />
          </>
        ) : (
          <EmptyState
            icon={ClipboardList}
            title="No requirements found"
            description="Try clearing the filters, or add a requirement from a lead's Requirements tab."
          />
        )}
      </Card>
    </div>
  );
}

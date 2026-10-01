import { useMemo, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { Building2, CheckCircle2, Clock, Plus, Search } from "lucide-react";
import { PageHeader } from "@/components/common/PageHeader";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/common/EmptyState";
import { Pagination } from "@/components/common/Pagination";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from "@/components/ui/table";
import { ApprovalStatusBadge, NatureOfBusinessBadge } from "@/components/customers/CustomerBadges";
import { CustomerFormDialog } from "@/components/customers/CustomerFormDialog";
import { customersApi } from "@/api/customers";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import { formatDate } from "@/lib/utils";
import { APPROVAL_STATUSES, APPROVAL_STATUS_LABELS, type ApprovalStatus } from "@/types/customer";

const PAGE_SIZE = 20;

function StatCard({
  icon: Icon,
  label,
  value,
  loading,
}: {
  icon: React.ComponentType<{ className?: string }>;
  label: string;
  value: React.ReactNode;
  loading: boolean;
}) {
  return (
    <Card>
      <CardContent className="flex items-center justify-between p-5">
        <div>
          <p className="text-sm text-muted-foreground">{label}</p>
          {loading ? <Skeleton className="mt-2 h-7 w-14" /> : <p className="mt-1 text-2xl font-semibold">{value}</p>}
        </div>
        <div className="flex h-10 w-10 items-center justify-center rounded-full bg-accent">
          <Icon className="h-5 w-5 text-accent-foreground" />
        </div>
      </CardContent>
    </Card>
  );
}

export function CustomersPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [page, setPage] = useState(0);
  const [approvalStatus, setApprovalStatus] = useState<ApprovalStatus | "ALL">("ALL");
  const [search, setSearch] = useState("");
  const debouncedSearch = useDebouncedValue(search, 250);
  const [createOpen, setCreateOpen] = useState(false);

  const customersQuery = useQuery({
    queryKey: ["customers", { page, approvalStatus }],
    queryFn: () =>
      customersApi.list({
        page,
        size: PAGE_SIZE,
        approvalStatus: approvalStatus === "ALL" ? undefined : approvalStatus,
      }),
  });

  // Cheap totals for the stat strip - size:1 pages just to read totalElements,
  // same trick DashboardPage already uses for its lead count.
  const pendingQuery = useQuery({
    queryKey: ["customers", { page: 0, size: 1, approvalStatus: "PENDING" }],
    queryFn: () => customersApi.list({ page: 0, size: 1, approvalStatus: "PENDING" }),
  });
  const approvedQuery = useQuery({
    queryKey: ["customers", { page: 0, size: 1, approvalStatus: "APPROVED" }],
    queryFn: () => customersApi.list({ page: 0, size: 1, approvalStatus: "APPROVED" }),
  });

  // In-page text filter, applied client-side on top of the server-paged
  // approval-status filter - same pattern LeadsPage uses for its own search box.
  const visibleCustomers = useMemo(() => {
    const content = customersQuery.data?.content ?? [];
    const q = debouncedSearch.trim().toLowerCase();
    if (!q) return content;
    return content.filter(
      (c) =>
        c.companyName.toLowerCase().includes(q) ||
        c.gstNo?.toLowerCase().includes(q) ||
        c.contacts.some((contact) => contact.name.toLowerCase().includes(q)),
    );
  }, [customersQuery.data, debouncedSearch]);

  return (
    <div>
      <PageHeader
        title="Customers"
        description="Every company you do business with, one row per customer."
        actions={
          <Button onClick={() => setCreateOpen(true)}>
            <Plus className="h-4 w-4" />
            New Customer
          </Button>
        }
      />

      <div className="mb-4 grid grid-cols-1 gap-4 sm:grid-cols-3">
        <StatCard
          icon={Building2}
          label="Total customers"
          value={customersQuery.data?.totalElements ?? 0}
          loading={customersQuery.isLoading}
        />
        <StatCard
          icon={Clock}
          label="Pending approval"
          value={pendingQuery.data?.totalElements ?? 0}
          loading={pendingQuery.isLoading}
        />
        <StatCard
          icon={CheckCircle2}
          label="Approved"
          value={approvedQuery.data?.totalElements ?? 0}
          loading={approvedQuery.isLoading}
        />
      </div>

      <Card>
        <div className="flex flex-wrap items-center gap-2 border-b border-border p-3">
          <div className="relative w-full max-w-xs">
            <Search className="pointer-events-none absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Filter this page by company, GST, contact…"
              className="pl-8"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
          <Select
            value={approvalStatus}
            onValueChange={(v) => {
              setApprovalStatus(v as ApprovalStatus | "ALL");
              setPage(0);
            }}
          >
            <SelectTrigger className="w-44">
              <SelectValue placeholder="Approval status" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">All statuses</SelectItem>
              {APPROVAL_STATUSES.map((s) => (
                <SelectItem key={s} value={s}>
                  {APPROVAL_STATUS_LABELS[s]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        {customersQuery.isLoading ? (
          <div className="space-y-2 p-4">
            {Array.from({ length: 6 }).map((_, i) => (
              <Skeleton key={i} className="h-10 w-full" />
            ))}
          </div>
        ) : visibleCustomers.length === 0 ? (
          <EmptyState
            icon={Building2}
            title="No customers to show"
            description="Try a different filter, or create your first customer."
            action={
              <Button size="sm" className="mt-1" onClick={() => setCreateOpen(true)}>
                <Plus className="h-4 w-4" />
                New Customer
              </Button>
            }
          />
        ) : (
          <>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Company</TableHead>
                  <TableHead>Nature of business</TableHead>
                  <TableHead>Contact</TableHead>
                  <TableHead>Account manager</TableHead>
                  <TableHead>Approval</TableHead>
                  <TableHead>Created</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {visibleCustomers.map((customer) => (
                  <TableRow
                    key={customer.id}
                    className="cursor-pointer"
                    onClick={() => navigate(`/customers/${customer.id}`)}
                  >
                    <TableCell>
                      <p className="font-medium">{customer.companyName}</p>
                      <p className="text-xs text-muted-foreground">{customer.gstNo ?? "No GST on file"}</p>
                    </TableCell>
                    <TableCell>
                      <NatureOfBusinessBadge nature={customer.natureOfBusiness} />
                    </TableCell>
                    <TableCell className="text-muted-foreground">{customer.contacts[0]?.name ?? "—"}</TableCell>
                    <TableCell className="text-muted-foreground">
                      {customer.accountManagerName ?? "Unassigned"}
                    </TableCell>
                    <TableCell>
                      <ApprovalStatusBadge status={customer.approvalStatus} />
                    </TableCell>
                    <TableCell className="text-muted-foreground">{formatDate(customer.createdAt)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            {customersQuery.data && (
              <Pagination
                page={customersQuery.data.page}
                totalPages={customersQuery.data.totalPages}
                totalElements={customersQuery.data.totalElements}
                onPageChange={setPage}
              />
            )}
          </>
        )}
      </Card>

      <CustomerFormDialog
        open={createOpen}
        onOpenChange={setCreateOpen}
        onSaved={() => queryClient.invalidateQueries({ queryKey: ["customers"] })}
      />
    </div>
  );
}

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import {
  BadgeCheck,
  Building2,
  Clock,
  MoreHorizontal,
  Plus,
  Search,
  ShieldOff,
  Truck,
} from "lucide-react";
import { PageHeader } from "@/components/common/PageHeader";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/common/EmptyState";
import { Pagination } from "@/components/common/Pagination";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from "@/components/ui/table";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { SupplierCategoryBadge, SupplierStatusBadge, TrustScoreBar } from "@/components/suppliers/SupplierBadges";
import { SupplierFormDialog } from "@/components/suppliers/SupplierFormDialog";
import { suppliersApi } from "@/api/suppliers";
import { extractErrorMessage } from "@/api/client";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import {
  SUPPLIER_CATEGORIES,
  SUPPLIER_CATEGORY_LABELS,
  SUPPLIER_STATUSES,
  SUPPLIER_STATUS_LABELS,
  type SupplierCategory,
  type SupplierResponse,
  type SupplierStatus,
} from "@/types/supplier";

const PAGE_SIZE = 20;

export function SuppliersPage() {
  const queryClient = useQueryClient();

  const [page, setPage] = useState(0);
  const [status, setStatus] = useState<SupplierStatus | "ALL">("ALL");
  const [category, setCategory] = useState<SupplierCategory | "ALL">("ALL");
  const [search, setSearch] = useState("");
  const debouncedSearch = useDebouncedValue(search, 300);

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<SupplierResponse | undefined>(undefined);

  const summaryQuery = useQuery({ queryKey: ["suppliers", "summary"], queryFn: suppliersApi.summary });

  const suppliersQuery = useQuery({
    queryKey: ["suppliers", { page, status, category, debouncedSearch }],
    queryFn: () =>
      suppliersApi.list({
        page,
        size: PAGE_SIZE,
        status: status === "ALL" ? undefined : status,
        category: category === "ALL" ? undefined : category,
        q: debouncedSearch,
      }),
  });

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: string; status: SupplierStatus }) => suppliersApi.changeStatus(id, status),
    onSuccess: () => {
      toast.success("Status updated");
      queryClient.invalidateQueries({ queryKey: ["suppliers"] });
    },
    onError: (error) => toast.error(extractErrorMessage(error, "Couldn't update status")),
  });

  const archiveMutation = useMutation({
    mutationFn: (id: string) => suppliersApi.archive(id),
    onSuccess: () => {
      toast.success("Supplier archived");
      queryClient.invalidateQueries({ queryKey: ["suppliers"] });
    },
    onError: (error) => toast.error(extractErrorMessage(error, "Couldn't archive supplier")),
  });

  function openCreate() {
    setEditing(undefined);
    setFormOpen(true);
  }

  function openEdit(supplier: SupplierResponse) {
    setEditing(supplier);
    setFormOpen(true);
  }

  const suppliers = suppliersQuery.data?.content ?? [];
  const summary = summaryQuery.data;

  return (
    <div>
      <PageHeader
        title="Suppliers"
        description="Your vendor master data — onboarding, verification, and procurement details."
        actions={
          <Button onClick={openCreate}>
            <Plus className="h-4 w-4" />
            Add supplier
          </Button>
        }
      />

      <div className="mb-5 grid grid-cols-2 gap-3 sm:grid-cols-4">
        <StatCard icon={Truck} label="Total suppliers" value={summary?.total} loading={summaryQuery.isLoading} />
        <StatCard
          icon={BadgeCheck}
          label="Verified"
          value={summary?.verified}
          loading={summaryQuery.isLoading}
          accent="text-emerald-600"
        />
        <StatCard
          icon={Clock}
          label="Pending verification"
          value={summary?.pendingVerification}
          loading={summaryQuery.isLoading}
          accent="text-amber-600"
        />
        <StatCard
          icon={ShieldOff}
          label="On hold / blacklisted"
          value={summary?.onHoldOrBlacklisted}
          loading={summaryQuery.isLoading}
          accent="text-red-600"
        />
      </div>

      <Card>
        <div className="flex flex-wrap items-center gap-2 border-b border-border p-3">
          <div className="relative w-full max-w-xs">
            <Search className="pointer-events-none absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Search name, GST, phone, code…"
              className="pl-8"
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
              setStatus(v as SupplierStatus | "ALL");
              setPage(0);
            }}
          >
            <SelectTrigger className="w-44">
              <SelectValue placeholder="Status" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">All statuses</SelectItem>
              {SUPPLIER_STATUSES.map((s) => (
                <SelectItem key={s} value={s}>
                  {SUPPLIER_STATUS_LABELS[s]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Select
            value={category}
            onValueChange={(v) => {
              setCategory(v as SupplierCategory | "ALL");
              setPage(0);
            }}
          >
            <SelectTrigger className="w-44">
              <SelectValue placeholder="Category" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">All categories</SelectItem>
              {SUPPLIER_CATEGORIES.map((c) => (
                <SelectItem key={c} value={c}>
                  {SUPPLIER_CATEGORY_LABELS[c]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        {suppliersQuery.isLoading ? (
          <div className="space-y-2 p-4">
            {Array.from({ length: 6 }).map((_, i) => (
              <Skeleton key={i} className="h-10 w-full" />
            ))}
          </div>
        ) : suppliers.length === 0 ? (
          <EmptyState
            icon={Building2}
            title="No suppliers to show"
            description="Try a different filter, or add your first vendor."
            action={
              <Button size="sm" className="mt-1" onClick={openCreate}>
                <Plus className="h-4 w-4" />
                Add supplier
              </Button>
            }
          />
        ) : (
          <>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Supplier</TableHead>
                  <TableHead>Category</TableHead>
                  <TableHead>Contact</TableHead>
                  <TableHead>GST</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Profile strength</TableHead>
                  <TableHead />
                </TableRow>
              </TableHeader>
              <TableBody>
                {suppliers.map((s) => (
                  <TableRow key={s.id} className="cursor-pointer" onClick={() => openEdit(s)}>
                    <TableCell>
                      <p className="font-medium">{s.companyName}</p>
                      <p className="text-xs text-muted-foreground">
                        {s.supplierCode} · {s.city ?? "—"}
                      </p>
                    </TableCell>
                    <TableCell>
                      <SupplierCategoryBadge category={s.category} />
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      <p>{s.contactName ?? "—"}</p>
                      <p className="text-xs">{s.contactPhone ?? s.contactEmail ?? ""}</p>
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {s.gstNumber ? (
                        <span className={s.gstVerified ? "text-emerald-700" : ""}>
                          {s.gstNumber}
                          {s.gstVerified && " ✓"}
                        </span>
                      ) : (
                        "—"
                      )}
                    </TableCell>
                    <TableCell>
                      <SupplierStatusBadge status={s.status} />
                    </TableCell>
                    <TableCell>
                      <TrustScoreBar score={s.trustScore} />
                    </TableCell>
                    <TableCell onClick={(e) => e.stopPropagation()}>
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button variant="ghost" size="icon">
                            <MoreHorizontal className="h-4 w-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <DropdownMenuItem onClick={() => openEdit(s)}>Edit details</DropdownMenuItem>
                          <DropdownMenuSeparator />
                          {SUPPLIER_STATUSES.filter((st) => st !== s.status).map((st) => (
                            <DropdownMenuItem
                              key={st}
                              onClick={() => statusMutation.mutate({ id: s.id, status: st })}
                            >
                              Mark as {SUPPLIER_STATUS_LABELS[st]}
                            </DropdownMenuItem>
                          ))}
                          <DropdownMenuSeparator />
                          <DropdownMenuItem
                            className="text-destructive focus:text-destructive"
                            onClick={() => archiveMutation.mutate(s.id)}
                          >
                            Archive
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            {suppliersQuery.data && (
              <Pagination
                page={suppliersQuery.data.page}
                totalPages={suppliersQuery.data.totalPages}
                totalElements={suppliersQuery.data.totalElements}
                onPageChange={setPage}
              />
            )}
          </>
        )}
      </Card>

      <SupplierFormDialog
        open={formOpen}
        onOpenChange={setFormOpen}
        supplier={editing}
        onSaved={() => {
          queryClient.invalidateQueries({ queryKey: ["suppliers"] });
        }}
      />
    </div>
  );
}

function StatCard({
  icon: Icon,
  label,
  value,
  loading,
  accent = "text-primary",
}: {
  icon: React.ComponentType<{ className?: string }>;
  label: string;
  value?: number;
  loading?: boolean;
  accent?: string;
}) {
  return (
    <Card className="flex items-center gap-3 p-4">
      <div className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-accent ${accent}`}>
        <Icon className="h-[18px] w-[18px]" />
      </div>
      <div>
        {loading ? (
          <Skeleton className="h-6 w-10" />
        ) : (
          <p className="text-xl font-semibold leading-tight">{value ?? 0}</p>
        )}
        <p className="text-xs text-muted-foreground">{label}</p>
      </div>
    </Card>
  );
}

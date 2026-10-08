import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { CheckCircle2, Clock, FileText, IndianRupee, MoreHorizontal, Plus, Search } from "lucide-react";
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
import { QuotationStatusBadge } from "@/components/quotations/QuotationBadges";
import { quotationsApi } from "@/api/quotations";
import { extractErrorMessage } from "@/api/client";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import { QUOTATION_STATUSES, QUOTATION_STATUS_LABELS, type QuotationStatus } from "@/types/quotation";

const PAGE_SIZE = 20;

export function QuotationsPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [page, setPage] = useState(0);
  const [status, setStatus] = useState<QuotationStatus | "ALL">("ALL");
  const [search, setSearch] = useState("");
  const debouncedSearch = useDebouncedValue(search, 300);

  const summaryQuery = useQuery({ queryKey: ["quotations", "summary"], queryFn: quotationsApi.summary });
  const quotationsQuery = useQuery({
    queryKey: ["quotations", { page, status, debouncedSearch }],
    queryFn: () =>
      quotationsApi.list({
        page,
        size: PAGE_SIZE,
        status: status === "ALL" ? undefined : status,
        q: debouncedSearch,
      }),
  });

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: string; status: QuotationStatus }) => quotationsApi.changeStatus(id, status),
    onSuccess: () => {
      toast.success("Status updated");
      queryClient.invalidateQueries({ queryKey: ["quotations"] });
    },
    onError: (error) => toast.error(extractErrorMessage(error, "Couldn't update status")),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => quotationsApi.remove(id),
    onSuccess: () => {
      toast.success("Draft deleted");
      queryClient.invalidateQueries({ queryKey: ["quotations"] });
    },
    onError: (error) => toast.error(extractErrorMessage(error, "Couldn't delete this quotation")),
  });

  const quotations = quotationsQuery.data?.content ?? [];
  const summary = summaryQuery.data;

  return (
    <div>
      <PageHeader
        title="Quotations"
        description="Every customer quotation, its price trail, and where it stands."
        actions={
          <Button onClick={() => navigate("/quotations/new")}>
            <Plus className="h-4 w-4" />
            New quotation
          </Button>
        }
      />

      <div className="mb-5 grid grid-cols-2 gap-3 sm:grid-cols-4">
        <StatCard icon={FileText} label="Total quotations" value={summary?.total} loading={summaryQuery.isLoading} />
        <StatCard
          icon={Clock}
          label="Pending (sent / review)"
          value={summary?.pending}
          loading={summaryQuery.isLoading}
          accent="text-amber-600"
        />
        <StatCard
          icon={CheckCircle2}
          label="Accepted"
          value={summary?.accepted}
          loading={summaryQuery.isLoading}
          accent="text-emerald-600"
        />
        <StatCard
          icon={IndianRupee}
          label="Accepted value"
          value={summary?.acceptedValue}
          format={(v) => v.toLocaleString("en-IN", { maximumFractionDigits: 0 })}
          loading={summaryQuery.isLoading}
          accent="text-emerald-600"
        />
      </div>

      <Card>
        <div className="flex flex-wrap items-center gap-2 border-b border-border p-3">
          <div className="relative w-full max-w-xs">
            <Search className="pointer-events-none absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Search number, customer or lead…"
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
              setStatus(v as QuotationStatus | "ALL");
              setPage(0);
            }}
          >
            <SelectTrigger className="w-44">
              <SelectValue placeholder="Status" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">All statuses</SelectItem>
              {QUOTATION_STATUSES.map((s) => (
                <SelectItem key={s} value={s}>
                  {QUOTATION_STATUS_LABELS[s]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        {quotationsQuery.isLoading ? (
          <div className="space-y-2 p-4">
            {Array.from({ length: 6 }).map((_, i) => (
              <Skeleton key={i} className="h-10 w-full" />
            ))}
          </div>
        ) : quotations.length === 0 ? (
          <EmptyState
            icon={FileText}
            title="No quotations to show"
            description="Try a different filter, or create your first quotation."
            action={
              <Button size="sm" className="mt-1" onClick={() => navigate("/quotations/new")}>
                <Plus className="h-4 w-4" />
                New quotation
              </Button>
            }
          />
        ) : (
          <>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Quotation</TableHead>
                  <TableHead>Customer / lead</TableHead>
                  <TableHead>Date</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="text-right">Grand total</TableHead>
                  <TableHead />
                </TableRow>
              </TableHeader>
              <TableBody>
                {quotations.map((q) => (
                  <TableRow
                    key={q.id}
                    className="cursor-pointer"
                    onClick={() => navigate(`/quotations/${q.id}/edit`)}
                  >
                    <TableCell>
                      <p className="font-medium">{q.quotationNumber}</p>
                      <p className="text-xs text-muted-foreground">{q.lineItems.length} item(s)</p>
                    </TableCell>
                    <TableCell>
                      {q.displayName}
                      {!q.customerId && <span className="ml-1.5 text-xs text-muted-foreground">(lead)</span>}
                    </TableCell>
                    <TableCell className="text-muted-foreground">{q.quotationDate}</TableCell>
                    <TableCell>
                      <QuotationStatusBadge status={q.status} />
                    </TableCell>
                    <TableCell className="text-right font-medium tabular-nums">
                      {q.currency} {q.grandTotal.toFixed(2)}
                    </TableCell>
                    <TableCell onClick={(e) => e.stopPropagation()}>
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button variant="ghost" size="icon">
                            <MoreHorizontal className="h-4 w-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <DropdownMenuItem onClick={() => navigate(`/quotations/${q.id}/edit`)}>
                            Open
                          </DropdownMenuItem>
                          <DropdownMenuSeparator />
                          {QUOTATION_STATUSES.filter((st) => st !== q.status).map((st) => (
                            <DropdownMenuItem
                              key={st}
                              onClick={() => statusMutation.mutate({ id: q.id, status: st })}
                            >
                              Mark as {QUOTATION_STATUS_LABELS[st]}
                            </DropdownMenuItem>
                          ))}
                          {q.status === "DRAFT" && (
                            <>
                              <DropdownMenuSeparator />
                              <DropdownMenuItem
                                className="text-destructive focus:text-destructive"
                                onClick={() => deleteMutation.mutate(q.id)}
                              >
                                Delete draft
                              </DropdownMenuItem>
                            </>
                          )}
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            {quotationsQuery.data && (
              <Pagination
                page={quotationsQuery.data.page}
                totalPages={quotationsQuery.data.totalPages}
                totalElements={quotationsQuery.data.totalElements}
                onPageChange={setPage}
              />
            )}
          </>
        )}
      </Card>
    </div>
  );
}

function StatCard({
  icon: Icon,
  label,
  value,
  loading,
  accent = "text-primary",
  format,
}: {
  icon: React.ComponentType<{ className?: string }>;
  label: string;
  value?: number;
  loading?: boolean;
  accent?: string;
  format?: (v: number) => string;
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
          <p className="text-xl font-semibold leading-tight">{value != null ? (format ? format(value) : value) : 0}</p>
        )}
        <p className="text-xs text-muted-foreground">{label}</p>
      </div>
    </Card>
  );
}

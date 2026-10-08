import { type ReactNode, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Filter, TrendingDown, Radio, Truck, FileText, Users2, Sparkles, RotateCw } from "lucide-react";
import { PageHeader } from "@/components/common/PageHeader";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/common/EmptyState";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { BarList } from "@/components/dashboard/BarList";
import { DateRangeFilter, defaultDateRange } from "@/components/dashboard/DateRangeFilter";
import { reportsApi } from "@/api/reports";
import { extractErrorMessage } from "@/api/client";
import { useAuthStore } from "@/store/authStore";
import { formatCurrencyINR } from "@/lib/utils";
import { LEAD_STAGE_LABELS, LOST_REASON_LABELS } from "@/types/lead";
import {
  LOST_REASON_GROUP_LABELS,
  REPORT_GROUP_BY_OPTIONS,
  type ReportDateRange,
  type ReportGroupBy,
} from "@/types/report";

function ErrorRetry({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <div className="flex flex-col items-center gap-2 py-8 text-center text-sm text-muted-foreground">
      <p>{message}</p>
      <Button type="button" variant="outline" size="sm" onClick={onRetry}>
        <RotateCw className="mr-1.5 h-3.5 w-3.5" /> Retry
      </Button>
    </div>
  );
}

function WidgetSkeleton() {
  return (
    <div className="space-y-3">
      <Skeleton className="h-5 w-full" />
      <Skeleton className="h-5 w-5/6" />
      <Skeleton className="h-5 w-2/3" />
    </div>
  );
}

// ---- Funnel tab ----

function FunnelTab({ range }: { range: Required<ReportDateRange> }) {
  const query = useQuery({ queryKey: ["reports", "funnel", range], queryFn: () => reportsApi.funnel(range) });

  return (
    <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
      <Card>
        <CardHeader>
          <CardTitle className="text-base font-semibold text-foreground">By stage</CardTitle>
        </CardHeader>
        <CardContent>
          {query.isLoading ? (
            <WidgetSkeleton />
          ) : query.isError ? (
            <ErrorRetry message={extractErrorMessage(query.error)} onRetry={() => query.refetch()} />
          ) : query.data && query.data.byStage.length > 0 ? (
            <BarList
              items={query.data.byStage.map((b) => ({
                key: b.stage,
                label: LEAD_STAGE_LABELS[b.stage],
                value: b.count,
                secondary: formatCurrencyINR(b.value),
              }))}
            />
          ) : (
            <EmptyState title="No leads in this window" className="py-8" />
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base font-semibold text-foreground">By owner</CardTitle>
        </CardHeader>
        <CardContent>
          {query.isLoading ? (
            <WidgetSkeleton />
          ) : query.isError ? (
            <ErrorRetry message={extractErrorMessage(query.error)} onRetry={() => query.refetch()} />
          ) : query.data && query.data.byOwner.length > 0 ? (
            <BarList
              items={query.data.byOwner.map((b) => ({
                key: b.ownerId ?? "unassigned",
                label: b.ownerName,
                value: b.count,
                secondary: formatCurrencyINR(b.value),
              }))}
            />
          ) : (
            <EmptyState title="No leads in this window" className="py-8" />
          )}
        </CardContent>
      </Card>
    </div>
  );
}

// ---- Lost analysis tab ----

function LostAnalysisTab({ range }: { range: Required<ReportDateRange> }) {
  const query = useQuery({
    queryKey: ["reports", "lost-analysis", range],
    queryFn: () => reportsApi.lostAnalysis(range),
  });

  if (query.isLoading) {
    return (
      <Card>
        <CardContent className="p-5">
          <WidgetSkeleton />
        </CardContent>
      </Card>
    );
  }
  if (query.isError) {
    return (
      <Card>
        <CardContent className="p-5">
          <ErrorRetry message={extractErrorMessage(query.error)} onRetry={() => query.refetch()} />
        </CardContent>
      </Card>
    );
  }
  if (!query.data || query.data.totalLostCount === 0) {
    return (
      <Card>
        <CardContent className="p-5">
          <EmptyState icon={TrendingDown} title="Nothing lost in this window" className="py-8" />
        </CardContent>
      </Card>
    );
  }

  const { totalLostCount, totalLostValue, byReason, byGroup } = query.data;

  return (
    <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
      <Card>
        <CardHeader>
          <CardTitle className="text-base font-semibold text-foreground">
            By group — {totalLostCount} lost, {formatCurrencyINR(totalLostValue)}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <BarList
            items={byGroup.map((g) => ({
              key: g.group,
              label: LOST_REASON_GROUP_LABELS[g.group],
              value: g.count,
              secondary: formatCurrencyINR(g.value),
              barClassName: "bg-destructive",
            }))}
          />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base font-semibold text-foreground">By reason</CardTitle>
        </CardHeader>
        <CardContent>
          <BarList
            items={byReason
              .slice()
              .sort((a, b) => b.count - a.count)
              .map((r) => ({
                key: r.reason,
                label: LOST_REASON_LABELS[r.reason as keyof typeof LOST_REASON_LABELS] ?? r.reason,
                value: r.count,
                secondary: formatCurrencyINR(r.value),
                barClassName: "bg-destructive",
              }))}
          />
        </CardContent>
      </Card>
    </div>
  );
}

// ---- Sources tab ----

function SourcesTab({ range }: { range: Required<ReportDateRange> }) {
  const query = useQuery({
    queryKey: ["reports", "lead-sources", range],
    queryFn: () => reportsApi.leadSources(range),
  });

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base font-semibold text-foreground">Leads by source</CardTitle>
      </CardHeader>
      <CardContent>
        {query.isLoading ? (
          <WidgetSkeleton />
        ) : query.isError ? (
          <ErrorRetry message={extractErrorMessage(query.error)} onRetry={() => query.refetch()} />
        ) : query.data && query.data.length > 0 ? (
          <BarList
            items={query.data.map((s) => ({
              key: s.source,
              label: s.source,
              value: s.total,
              secondary: `${s.newCount} new · ${s.active} active · ${s.won} won`,
            }))}
          />
        ) : (
          <EmptyState icon={Radio} title="No leads in this window" className="py-8" />
        )}
      </CardContent>
    </Card>
  );
}

// ---- Suppliers tab ----

function SupplierStat({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="rounded-lg border border-border p-3">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="mt-1 text-xl font-semibold tabular-nums">{value}</p>
    </div>
  );
}

function SuppliersTab({ range }: { range: Required<ReportDateRange> }) {
  const query = useQuery({ queryKey: ["reports", "suppliers", range], queryFn: () => reportsApi.suppliers(range) });

  if (query.isLoading) {
    return (
      <Card>
        <CardContent className="p-5">
          <WidgetSkeleton />
        </CardContent>
      </Card>
    );
  }
  if (query.isError) {
    return (
      <Card>
        <CardContent className="p-5">
          <ErrorRetry message={extractErrorMessage(query.error)} onRetry={() => query.refetch()} />
        </CardContent>
      </Card>
    );
  }

  const data = query.data;

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <SupplierStat label="Active suppliers" value={data?.activeSuppliers ?? 0} />
        <SupplierStat label="Enquiries sent" value={data?.enquiriesSent ?? 0} />
        <SupplierStat label="Pending replies" value={data?.pendingReplies ?? 0} />
        <SupplierStat label="Replies received" value={data?.repliesReceived ?? 0} />
      </div>
      <Card>
        <CardHeader>
          <CardTitle className="text-base font-semibold text-foreground">Supplier-wise</CardTitle>
        </CardHeader>
        <CardContent>
          {data && data.bySupplier.length > 0 ? (
            <BarList
              items={data.bySupplier.map((s) => ({
                key: s.supplierId,
                label: s.supplierName,
                value: s.enquiries,
                secondary: `${s.replies} replied · ${s.chosen} chosen`,
              }))}
            />
          ) : (
            <EmptyState icon={Truck} title="No supplier enquiries in this window" className="py-8" />
          )}
        </CardContent>
      </Card>
    </div>
  );
}

// ---- Quotations tab ----

function QuotationsTab({ range }: { range: Required<ReportDateRange> }) {
  const query = useQuery({
    queryKey: ["reports", "quotations", range],
    queryFn: () => reportsApi.quotations(range),
  });

  if (query.isLoading) {
    return (
      <Card>
        <CardContent className="p-5">
          <WidgetSkeleton />
        </CardContent>
      </Card>
    );
  }
  if (query.isError) {
    return (
      <Card>
        <CardContent className="p-5">
          <ErrorRetry message={extractErrorMessage(query.error)} onRetry={() => query.refetch()} />
        </CardContent>
      </Card>
    );
  }
  if (!query.data || query.data.generated === 0) {
    return (
      <Card>
        <CardContent className="p-5">
          <EmptyState icon={FileText} title="No quotations in this window" className="py-8" />
        </CardContent>
      </Card>
    );
  }

  const q = query.data;
  return (
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
      <SupplierStat label="Generated" value={q.generated} />
      <SupplierStat label="Pending" value={q.pending} />
      <SupplierStat label="Accepted" value={`${q.accepted} · ${formatCurrencyINR(q.acceptedValue)}`} />
      <SupplierStat label="Rejected" value={q.rejected} />
      <SupplierStat label="Expired" value={q.expired} />
    </div>
  );
}

// ---- Team tab ----

function TeamTab({ range }: { range: Required<ReportDateRange> }) {
  const query = useQuery({
    queryKey: ["reports", "team-performance", range],
    queryFn: () => reportsApi.teamPerformance(range),
  });

  if (query.isLoading) {
    return (
      <Card>
        <CardContent className="p-5">
          <WidgetSkeleton />
        </CardContent>
      </Card>
    );
  }
  if (query.isError) {
    return (
      <Card>
        <CardContent className="p-5">
          <ErrorRetry message={extractErrorMessage(query.error)} onRetry={() => query.refetch()} />
        </CardContent>
      </Card>
    );
  }
  if (!query.data || query.data.length === 0) {
    return (
      <Card>
        <CardContent className="p-5">
          <EmptyState icon={Users2} title="No team members in scope" className="py-8" />
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardContent className="overflow-x-auto p-0">
        <table className="w-full text-sm">
          <thead className="border-b border-border text-left text-xs text-muted-foreground">
            <tr>
              <th className="px-4 py-2 font-medium">Team member</th>
              <th className="px-4 py-2 font-medium">Assigned</th>
              <th className="px-4 py-2 font-medium">Calls</th>
              <th className="px-4 py-2 font-medium">Meetings</th>
              <th className="px-4 py-2 font-medium">Follow-ups</th>
              <th className="px-4 py-2 font-medium">Pending</th>
              <th className="px-4 py-2 font-medium">Quotations</th>
              <th className="px-4 py-2 font-medium">Won</th>
              <th className="px-4 py-2 font-medium">Lost</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {query.data.map((row) => (
              <tr key={row.userId}>
                <td className="px-4 py-2 font-medium">{row.userName}</td>
                <td className="px-4 py-2 tabular-nums">{row.assignedLeads}</td>
                <td className="px-4 py-2 tabular-nums">{row.callsCompleted}</td>
                <td className="px-4 py-2 tabular-nums">{row.meetingsCompleted}</td>
                <td className="px-4 py-2 tabular-nums">{row.followUpsCompleted}</td>
                <td className="px-4 py-2 tabular-nums">{row.pendingTasks}</td>
                <td className="px-4 py-2 tabular-nums">{row.quotationsHandled}</td>
                <td className="px-4 py-2 tabular-nums text-emerald-700">{row.won}</td>
                <td className="px-4 py-2 tabular-nums text-destructive">{row.lost}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </CardContent>
    </Card>
  );
}

// ---- Custom report builder + AI assistant (unchanged from before) ----

function ReportBuilderTab() {
  const [groupBy, setGroupBy] = useState<ReportGroupBy>("STAGE");
  const query = useQuery({ queryKey: ["reports", "leads", groupBy], queryFn: () => reportsApi.leadReport(groupBy) });

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0">
        <CardTitle className="text-base font-semibold text-foreground">Custom lead report</CardTitle>
        <Select value={groupBy} onValueChange={(v) => setGroupBy(v as ReportGroupBy)}>
          <SelectTrigger className="w-[180px]">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {REPORT_GROUP_BY_OPTIONS.map((o) => (
              <SelectItem key={o.value} value={o.value}>
                {o.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </CardHeader>
      <CardContent>
        {query.isLoading ? (
          <WidgetSkeleton />
        ) : query.isError ? (
          <ErrorRetry message={extractErrorMessage(query.error)} onRetry={() => query.refetch()} />
        ) : query.data && Object.keys(query.data).length > 0 ? (
          <BarList
            items={Object.entries(query.data).map(([label, value]) => ({ key: label, label, value }))}
          />
        ) : (
          <EmptyState title="No data" className="py-8" />
        )}
      </CardContent>
    </Card>
  );
}

function AiAssistantTab() {
  const [question, setQuestion] = useState("");
  const ask = useMutation({ mutationFn: (q: string) => reportsApi.ask(q) });

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-base font-semibold text-foreground">
          <Sparkles className="h-4 w-4 text-primary" />
          Ask about your pipeline
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">
        <Textarea
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          placeholder="e.g. Which sources are converting best this month?"
          rows={2}
        />
        <Button
          type="button"
          size="sm"
          disabled={!question.trim() || ask.isPending}
          onClick={() => ask.mutate(question)}
        >
          {ask.isPending ? "Thinking…" : "Ask"}
        </Button>
        {ask.isError && <ErrorRetry message={extractErrorMessage(ask.error)} onRetry={() => ask.mutate(question)} />}
        {ask.data && <p className="text-sm leading-relaxed">{ask.data.narrative}</p>}
      </CardContent>
    </Card>
  );
}

// ---- Page ----

export function ReportsPage() {
  const role = useAuthStore((s) => s.user?.role);
  const [range, setRange] = useState(defaultDateRange());
  // Assumption: VIEWER is a read-only/observer role with no "my own performance"
  // concept in the PRD, so Team is hidden for it specifically (task #5 says
  // "hidden for roles without access" without naming which - every other role
  // has at least a narrowed view of it per task #1's scope rule).
  const showTeamTab = role !== "VIEWER";

  return (
    <div>
      <PageHeader title="Reports" description="Pipeline, suppliers and quotations, for the selected window." />

      <div className="mb-4">
        <DateRangeFilter value={range} onChange={setRange} />
      </div>

      <Tabs defaultValue="funnel">
        <TabsList className="flex-wrap">
          <TabsTrigger value="funnel">
            <Filter className="mr-1.5 h-3.5 w-3.5" /> Funnel
          </TabsTrigger>
          <TabsTrigger value="lost">
            <TrendingDown className="mr-1.5 h-3.5 w-3.5" /> Lost analysis
          </TabsTrigger>
          <TabsTrigger value="sources">
            <Radio className="mr-1.5 h-3.5 w-3.5" /> Sources
          </TabsTrigger>
          <TabsTrigger value="suppliers">
            <Truck className="mr-1.5 h-3.5 w-3.5" /> Suppliers
          </TabsTrigger>
          <TabsTrigger value="quotations">
            <FileText className="mr-1.5 h-3.5 w-3.5" /> Quotations
          </TabsTrigger>
          {showTeamTab && (
            <TabsTrigger value="team">
              <Users2 className="mr-1.5 h-3.5 w-3.5" /> Team
            </TabsTrigger>
          )}
          <TabsTrigger value="builder">Custom report</TabsTrigger>
          <TabsTrigger value="ai">AI assistant</TabsTrigger>
        </TabsList>

        <TabsContent value="funnel" className="mt-4">
          <FunnelTab range={range} />
        </TabsContent>
        <TabsContent value="lost" className="mt-4">
          <LostAnalysisTab range={range} />
        </TabsContent>
        <TabsContent value="sources" className="mt-4">
          <SourcesTab range={range} />
        </TabsContent>
        <TabsContent value="suppliers" className="mt-4">
          <SuppliersTab range={range} />
        </TabsContent>
        <TabsContent value="quotations" className="mt-4">
          <QuotationsTab range={range} />
        </TabsContent>
        {showTeamTab && (
          <TabsContent value="team" className="mt-4">
            <TeamTab range={range} />
          </TabsContent>
        )}
        <TabsContent value="builder" className="mt-4">
          <ReportBuilderTab />
        </TabsContent>
        <TabsContent value="ai" className="mt-4">
          <AiAssistantTab />
        </TabsContent>
      </Tabs>
    </div>
  );
}

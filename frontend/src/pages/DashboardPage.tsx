import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import {
  Activity,
  AlertTriangle,
  ArrowUpRight,
  BarChart3,
  CheckSquare,
  Flame,
  IndianRupee,
  MessageCircle,
  Sparkles,
  Trophy,
  Users2,
} from "lucide-react";
import { PageHeader } from "@/components/common/PageHeader";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/common/EmptyState";
import { StageDonutChart } from "@/components/dashboard/StageDonutChart";
import { StageBadge, BuyingIntentBadge, ScoreBadge } from "@/components/leads/LeadBadges";
import { leadsApi } from "@/api/leads";
import { tasksApi } from "@/api/tasks";
import { activitiesApi } from "@/api/activities";
import { reportsApi } from "@/api/reports";
import { formatCurrencyINR, formatRelativeDay, initials } from "@/lib/utils";
import { useAuthStore } from "@/store/authStore";
import type { LeadStage } from "@/types/lead";

const QUICK_LINKS = [
  { to: "/leads", label: "Leads", icon: Users2 },
  { to: "/tasks", label: "Tasks", icon: CheckSquare },
  { to: "/activities", label: "Activities", icon: Activity },
  { to: "/whatsapp", label: "WhatsApp", icon: MessageCircle },
  { to: "/reports", label: "Reports", icon: BarChart3 },
];

function StatCard({
  icon: Icon,
  iconClassName,
  label,
  value,
  subtext,
  loading,
}: {
  icon: React.ComponentType<{ className?: string }>;
  iconClassName?: string;
  label: string;
  value: React.ReactNode;
  subtext?: React.ReactNode;
  loading: boolean;
}) {
  return (
    <Card>
      <CardContent className="flex items-start justify-between gap-3 p-5">
        <div className="min-w-0">
          <p className="text-sm text-muted-foreground">{label}</p>
          {loading ? (
            <Skeleton className="mt-2 h-7 w-16" />
          ) : (
            <p className="mt-1 truncate text-2xl font-semibold tabular-nums">{value}</p>
          )}
          {!loading && subtext && <p className="mt-1 text-xs text-muted-foreground">{subtext}</p>}
        </div>
        <div
          className={
            iconClassName ??
            "flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-accent text-accent-foreground"
          }
        >
          <Icon className="h-5 w-5" />
        </div>
      </CardContent>
    </Card>
  );
}

function iconChip(classes: string) {
  return `flex h-10 w-10 shrink-0 items-center justify-center rounded-full ${classes}`;
}

export function DashboardPage() {
  const user = useAuthStore((s) => s.user);
  const firstName = user?.fullName?.split(" ")[0];

  const summaryQuery = useQuery({ queryKey: ["reports", "dashboard-summary"], queryFn: reportsApi.dashboardSummary });
  const stageReportQuery = useQuery({
    queryKey: ["reports", "leads", "STAGE"],
    queryFn: () => reportsApi.leadReport("STAGE"),
  });
  const recentLeadsQuery = useQuery({
    queryKey: ["leads", { page: 0, size: 6 }],
    queryFn: () => leadsApi.list({ page: 0, size: 6 }),
  });
  const taskSummaryQuery = useQuery({ queryKey: ["tasks", "summary"], queryFn: tasksApi.summary });
  const activitySummaryQuery = useQuery({ queryKey: ["activities", "summary"], queryFn: activitiesApi.summary });
  const atRiskQuery = useQuery({ queryKey: ["leads", "at-risk"], queryFn: leadsApi.atRisk });
  const performanceQuery = useQuery({
    queryKey: ["reports", "counselor-performance"],
    queryFn: reportsApi.counselorPerformance,
  });

  // Fires automatically on load, no prompt needed, so the dashboard always
  // opens with a live read on the pipeline rather than just raw counts.
  const aiBriefingQuery = useQuery({
    queryKey: ["reports", "ask", "dashboard-briefing"],
    queryFn: () => reportsApi.ask("Give me a quick, plain-language summary of my pipeline right now."),
    staleTime: 5 * 60 * 1000,
  });

  const summary = summaryQuery.data;

  return (
    <div>
      <PageHeader
        title={`Welcome back${firstName ? `, ${firstName}` : ""}`}
        description="Here's what's moving across your pipeline today."
      />

      {/* Quick access */}
      <div className="mb-4 grid grid-cols-3 gap-2 sm:grid-cols-5">
        {QUICK_LINKS.map((link) => (
          <Link
            key={link.to}
            to={link.to}
            className="flex flex-col items-center gap-1.5 rounded-lg border border-border bg-card px-3 py-3 text-center text-xs font-medium text-foreground transition-colors hover:border-primary/40 hover:bg-accent hover:text-accent-foreground"
          >
            <link.icon className="h-4 w-4" />
            {link.label}
          </Link>
        ))}
      </div>

      {/* KPI row */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6">
        <StatCard
          icon={Users2}
          iconClassName={iconChip("bg-accent text-accent-foreground")}
          label="Total leads"
          value={summary?.totalLeads ?? 0}
          subtext={summary ? `${summary.openLeads} open` : undefined}
          loading={summaryQuery.isLoading}
        />
        <StatCard
          icon={IndianRupee}
          iconClassName={iconChip("bg-emerald-50 text-emerald-700")}
          label="Open pipeline"
          value={formatCurrencyINR(summary?.openPipelineValue ?? 0)}
          subtext="estimated value, open leads"
          loading={summaryQuery.isLoading}
        />
        <StatCard
          icon={Flame}
          iconClassName={iconChip("bg-red-50 text-red-600")}
          label="Hot leads"
          value={summary?.hotLeads ?? 0}
          subtext={summary ? `avg score ${summary.avgScore}` : undefined}
          loading={summaryQuery.isLoading}
        />
        <StatCard
          icon={AlertTriangle}
          iconClassName={iconChip("bg-amber-50 text-amber-700")}
          label="At-risk leads"
          value={atRiskQuery.data?.length ?? 0}
          subtext="inactive too long"
          loading={atRiskQuery.isLoading}
        />
        <StatCard
          icon={CheckSquare}
          iconClassName={iconChip("bg-sky-50 text-sky-700")}
          label="Pending tasks"
          value={taskSummaryQuery.data?.pending ?? 0}
          subtext={taskSummaryQuery.data ? `${taskSummaryQuery.data.overdue} overdue` : undefined}
          loading={taskSummaryQuery.isLoading}
        />
        <StatCard
          icon={Activity}
          iconClassName={iconChip("bg-violet-50 text-violet-700")}
          label="Activities logged"
          value={activitySummaryQuery.data?.total ?? 0}
          subtext={activitySummaryQuery.data ? `${activitySummaryQuery.data.completed} completed` : undefined}
          loading={activitySummaryQuery.isLoading}
        />
      </div>

      <div className="mt-4 grid grid-cols-1 gap-4 lg:grid-cols-3">
        {/* Left: pipeline + recent leads */}
        <div className="space-y-4 lg:col-span-2">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0">
              <CardTitle className="text-base font-semibold text-foreground">Pipeline by stage</CardTitle>
              <Link
                to="/reports"
                className="flex items-center gap-1 text-xs font-medium text-primary hover:underline"
              >
                Full report <ArrowUpRight className="h-3 w-3" />
              </Link>
            </CardHeader>
            <CardContent>
              {stageReportQuery.isLoading ? (
                <div className="flex items-center gap-5">
                  <Skeleton className="h-[176px] w-[176px] shrink-0 rounded-full" />
                  <div className="flex-1 space-y-2">
                    {Array.from({ length: 6 }).map((_, i) => (
                      <Skeleton key={i} className="h-6 w-full" />
                    ))}
                  </div>
                </div>
              ) : (
                <StageDonutChart counts={(stageReportQuery.data as Record<LeadStage, number>) ?? {}} />
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0">
              <CardTitle className="text-base font-semibold text-foreground">Recent leads</CardTitle>
              <Link to="/leads" className="flex items-center gap-1 text-xs font-medium text-primary hover:underline">
                View all <ArrowUpRight className="h-3 w-3" />
              </Link>
            </CardHeader>
            <CardContent className="p-0">
              {recentLeadsQuery.isLoading ? (
                <div className="space-y-2 p-5">
                  <Skeleton className="h-12 w-full" />
                  <Skeleton className="h-12 w-full" />
                  <Skeleton className="h-12 w-full" />
                </div>
              ) : recentLeadsQuery.data && recentLeadsQuery.data.content.length > 0 ? (
                <div className="divide-y divide-border">
                  {recentLeadsQuery.data.content.map((lead) => (
                    <Link
                      key={lead.id}
                      to={`/leads/${lead.id}`}
                      className="flex items-center gap-3 px-5 py-3 text-sm hover:bg-muted/50"
                    >
                      <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-accent text-xs font-semibold text-accent-foreground">
                        {initials(lead.fullName)}
                      </div>
                      <div className="min-w-0 flex-1">
                        <p className="truncate font-medium">{lead.fullName}</p>
                        <p className="truncate text-xs text-muted-foreground">{lead.interest ?? lead.city ?? "—"}</p>
                      </div>
                      <div className="hidden shrink-0 sm:block">
                        <StageBadge stage={lead.stage} />
                      </div>
                      {lead.buyingIntent && (
                        <div className="hidden shrink-0 md:block">
                          <BuyingIntentBadge intent={lead.buyingIntent} />
                        </div>
                      )}
                      <ScoreBadge score={lead.score} />
                    </Link>
                  ))}
                </div>
              ) : (
                <EmptyState
                  icon={Users2}
                  title="No leads yet"
                  description="New leads will show up here as soon as they're added."
                />
              )}
            </CardContent>
          </Card>
        </div>

        {/* Right: AI insights, at-risk, top performers */}
        <div className="space-y-4">
          <Card>
            <CardHeader className="space-y-0">
              <CardTitle className="flex items-center gap-2 text-base font-semibold text-foreground">
                <Sparkles className="h-4 w-4 text-primary" />
                AI pipeline briefing
              </CardTitle>
            </CardHeader>
            <CardContent>
              {aiBriefingQuery.isLoading ? (
                <div className="space-y-2">
                  <p className="text-xs text-muted-foreground">Analyzing your pipeline…</p>
                  <Skeleton className="h-4 w-full" />
                  <Skeleton className="h-4 w-5/6" />
                  <Skeleton className="h-4 w-2/3" />
                </div>
              ) : (
                <p className="text-sm leading-relaxed text-foreground">
                  {aiBriefingQuery.data?.narrative ?? "Couldn't generate a briefing right now."}
                </p>
              )}
              <Link
                to="/reports"
                className="mt-3 inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline"
              >
                Ask a different question <ArrowUpRight className="h-3 w-3" />
              </Link>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="space-y-0">
              <CardTitle className="flex items-center gap-2 text-base font-semibold text-foreground">
                <AlertTriangle className="h-4 w-4 text-amber-500" />
                At-risk leads
              </CardTitle>
            </CardHeader>
            <CardContent className="p-0">
              {atRiskQuery.isLoading ? (
                <div className="space-y-2 p-5">
                  <Skeleton className="h-10 w-full" />
                  <Skeleton className="h-10 w-full" />
                </div>
              ) : atRiskQuery.data && atRiskQuery.data.length > 0 ? (
                <div className="divide-y divide-border">
                  {atRiskQuery.data.slice(0, 5).map((lead) => (
                    <Link
                      key={lead.id}
                      to={`/leads/${lead.id}`}
                      className="flex items-center justify-between gap-2 px-5 py-2.5 text-sm hover:bg-muted/50"
                    >
                      <div className="min-w-0">
                        <p className="truncate font-medium">{lead.fullName}</p>
                        <p className="truncate text-xs text-muted-foreground">{lead.interest ?? "—"}</p>
                      </div>
                      <div className="shrink-0 text-right text-xs text-muted-foreground">
                        <p>{lead.daysSinceLastTouchpoint}d silent</p>
                        <p>next {formatRelativeDay(lead.nextFollowUpAt)}</p>
                      </div>
                    </Link>
                  ))}
                </div>
              ) : (
                <EmptyState
                  icon={AlertTriangle}
                  title="Nothing at risk"
                  description="Leads flagged here have gone quiet past your stale-lead threshold."
                  className="py-8"
                />
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="space-y-0">
              <CardTitle className="flex items-center gap-2 text-base font-semibold text-foreground">
                <Trophy className="h-4 w-4 text-amber-500" />
                Top performers
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              {performanceQuery.isLoading ? (
                <div className="space-y-2">
                  <Skeleton className="h-10 w-full" />
                  <Skeleton className="h-10 w-full" />
                </div>
              ) : performanceQuery.data && performanceQuery.data.length > 0 ? (
                performanceQuery.data.slice(0, 3).map((c) => (
                  <div key={c.ownerId}>
                    <div className="flex items-center justify-between text-sm">
                      <span className="font-medium">{c.ownerName}</span>
                      <span className="text-xs text-muted-foreground">
                        {c.converted}/{c.assigned} won
                      </span>
                    </div>
                    <div className="mt-1.5 h-1.5 w-full overflow-hidden rounded-full bg-muted">
                      <div
                        className="h-full rounded-full bg-primary"
                        style={{ width: `${Math.min(100, c.conversionRatePercent)}%` }}
                      />
                    </div>
                  </div>
                ))
              ) : (
                <EmptyState title="No assigned leads yet" className="py-8" />
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
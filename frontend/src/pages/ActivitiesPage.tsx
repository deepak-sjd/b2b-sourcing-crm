import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Activity as ActivityIcon, Phone, Users, MapPin, Mail, MessageCircle, MoreHorizontal, Clock } from "lucide-react";
import { PageHeader } from "@/components/common/PageHeader";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/common/EmptyState";
import { Pagination } from "@/components/common/Pagination";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from "@/components/ui/table";
import { activitiesApi } from "@/api/activities";
import { formatDate } from "@/lib/utils";
import {
  ACTIVITY_TYPES, ACTIVITY_TYPE_LABELS, ACTIVITY_STATUSES,
  type Activity, type ActivityType, type ActivityStatus,
} from "@/types/activity";

const PAGE_SIZE = 20;

const TYPE_ICON: Record<ActivityType, React.ComponentType<{ className?: string }>> = {
  CALL: Phone, MEETING: Users, SITE_VISIT: MapPin, EMAIL: Mail, WHATSAPP: MessageCircle, OTHER: MoreHorizontal,
};

const STATUS_CLASS: Record<ActivityStatus, string> = {
  SCHEDULED: "bg-accent text-accent-foreground",
  COMPLETED: "bg-emerald-100 text-emerald-700",
  MISSED: "bg-red-100 text-red-700",
  NO_ANSWER: "bg-amber-100 text-amber-800",
  BUSY: "bg-amber-100 text-amber-800",
};

function SummaryCard({ label, value, loading, accent }: { label: string; value: React.ReactNode; loading: boolean; accent?: boolean }) {
  return (
    <Card>
      <CardContent className="p-5">
        <p className="text-sm text-muted-foreground">{label}</p>
        {loading ? (
          <Skeleton className="mt-2 h-7 w-12" />
        ) : (
          <p className={`mt-1 text-2xl font-semibold ${accent ? "text-amber-600" : ""}`}>{value}</p>
        )}
      </CardContent>
    </Card>
  );
}

function DueSoonRow({ activity }: { activity: Activity }) {
  const Icon = TYPE_ICON[activity.activityType];
  const overdue = activity.followUpAt && new Date(activity.followUpAt).getTime() < Date.now();
  return (
    <div className="flex items-center justify-between gap-3 py-2">
      <div className="flex items-center gap-2.5">
        <Icon className="h-4 w-4 shrink-0 text-muted-foreground" />
        <div>
          <p className="text-sm font-medium">{activity.lead?.fullName ?? "—"}</p>
          <p className="text-xs text-muted-foreground">{ACTIVITY_TYPE_LABELS[activity.activityType]}</p>
        </div>
      </div>
      <span className={`inline-flex items-center gap-1 text-xs font-medium ${overdue ? "text-red-600" : "text-amber-700"}`}>
        <Clock className="h-3 w-3" />
        {formatDate(activity.followUpAt, true)}
      </span>
    </div>
  );
}

export function ActivitiesPage() {
  const [page, setPage] = useState(0);
  const [type, setType] = useState<ActivityType | "ALL">("ALL");
  const [status, setStatus] = useState<ActivityStatus | "ALL">("ALL");

  const summaryQuery = useQuery({ queryKey: ["activities", "summary"], queryFn: activitiesApi.summary });

  // Auto-loads on mount, no filter click needed — "what needs my attention
  // right now", independent of whatever the table below is filtered to.
  const dueSoonQuery = useQuery({ queryKey: ["activities", "due-follow-ups"], queryFn: () => activitiesApi.dueFollowUps(0, 5) });

  const activitiesQuery = useQuery({
    queryKey: ["activities", "list", { page, type, status }],
    queryFn: () =>
      activitiesApi.list({
        page, size: PAGE_SIZE,
        type: type === "ALL" ? undefined : type,
        status: status === "ALL" ? undefined : status,
      }),
  });

  return (
    <div>
      <PageHeader title="Activities" description="Every call, meeting, email, and touchpoint across your pipeline." />

      <div className="mb-4 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <SummaryCard label="Total activities" value={summaryQuery.data?.total ?? 0} loading={summaryQuery.isLoading} />
        <SummaryCard label="Completed" value={summaryQuery.data?.completed ?? 0} loading={summaryQuery.isLoading} />
        <SummaryCard label="Missed" value={summaryQuery.data?.missed ?? 0} loading={summaryQuery.isLoading} />
        <SummaryCard label="Due within 24h" value={summaryQuery.data?.dueToday ?? 0} loading={summaryQuery.isLoading} accent={(summaryQuery.data?.dueToday ?? 0) > 0} />
      </div>

      {!dueSoonQuery.isLoading && dueSoonQuery.data && dueSoonQuery.data.content.length > 0 && (
        <Card className="mb-4 border-amber-200 bg-amber-50/50">
          <CardContent className="p-4">
            <p className="mb-1 flex items-center gap-1.5 text-sm font-semibold text-amber-800">
              <Clock className="h-4 w-4" />
              Needs a follow-up soon
            </p>
            <div className="divide-y divide-amber-200/60">
              {dueSoonQuery.data.content.map((a) => <DueSoonRow key={a.id} activity={a} />)}
            </div>
          </CardContent>
        </Card>
      )}

      <Card>
        <div className="flex flex-wrap items-center gap-2 border-b border-border p-3">
          <Select value={type} onValueChange={(v) => { setType(v as ActivityType | "ALL"); setPage(0); }}>
            <SelectTrigger className="w-40"><SelectValue placeholder="Type" /></SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">All types</SelectItem>
              {ACTIVITY_TYPES.map((t) => <SelectItem key={t} value={t}>{ACTIVITY_TYPE_LABELS[t]}</SelectItem>)}
            </SelectContent>
          </Select>
          <Select value={status} onValueChange={(v) => { setStatus(v as ActivityStatus | "ALL"); setPage(0); }}>
            <SelectTrigger className="w-40"><SelectValue placeholder="Status" /></SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">All statuses</SelectItem>
              {ACTIVITY_STATUSES.map((s) => <SelectItem key={s} value={s}>{s.replaceAll("_", " ")}</SelectItem>)}
            </SelectContent>
          </Select>
        </div>

        {activitiesQuery.isLoading ? (
          <div className="space-y-2 p-4">
            {Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-10 w-full" />)}
          </div>
        ) : activitiesQuery.data && activitiesQuery.data.content.length > 0 ? (
          <>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Lead</TableHead>
                  <TableHead>Type</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>When</TableHead>
                  <TableHead>Follow up</TableHead>
                  <TableHead>Logged by</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {activitiesQuery.data.content.map((a) => {
                  const Icon = TYPE_ICON[a.activityType];
                  const overdue = a.followUpAt && new Date(a.followUpAt).getTime() < Date.now();
                  return (
                    <TableRow key={a.id}>
                      <TableCell className="font-medium">{a.lead?.fullName ?? "—"}</TableCell>
                      <TableCell>
                        <span className="inline-flex items-center gap-1.5 text-sm">
                          <Icon className="h-3.5 w-3.5 text-muted-foreground" />
                          {ACTIVITY_TYPE_LABELS[a.activityType]}
                        </span>
                      </TableCell>
                      <TableCell>
                        <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_CLASS[a.status]}`}>
                          {a.status.replaceAll("_", " ")}
                        </span>
                      </TableCell>
                      <TableCell className="text-muted-foreground">{formatDate(a.scheduledAt, true)}</TableCell>
                      <TableCell className={overdue ? "text-red-600" : "text-muted-foreground"}>
                        {a.followUpAt ? formatDate(a.followUpAt, true) : "—"}
                      </TableCell>
                      <TableCell className="text-muted-foreground">{a.loggedBy?.fullName ?? "—"}</TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
            <Pagination
              page={activitiesQuery.data.page}
              totalPages={activitiesQuery.data.totalPages}
              totalElements={activitiesQuery.data.totalElements}
              onPageChange={setPage}
            />
          </>
        ) : (
          <EmptyState icon={ActivityIcon} title="No activity to show" description="Try different filters, or log one from a lead's Activities tab." />
        )}
      </Card>
    </div>
  );
}
import { AlertTriangle, Flame, Gauge, Users2 } from "lucide-react";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";

interface StatProps {
  label: string;
  value: React.ReactNode;
  icon: React.ComponentType<{ className?: string }>;
  tone: "primary" | "hot" | "risk" | "neutral";
  loading?: boolean;
}

const TONE_CLASS: Record<StatProps["tone"], string> = {
  primary: "bg-primary/10 text-primary",
  hot: "bg-red-50 text-red-600",
  risk: "bg-amber-50 text-amber-700",
  neutral: "bg-muted text-muted-foreground",
};

function Stat({ label, value, icon: Icon, tone, loading }: StatProps) {
  return (
    <Card className="flex items-center gap-3 p-4">
      <div className={cn("flex h-10 w-10 shrink-0 items-center justify-center rounded-lg", TONE_CLASS[tone])}>
        <Icon className="h-5 w-5" />
      </div>
      <div className="min-w-0">
        <p className="text-xs font-medium text-muted-foreground">{label}</p>
        {loading ? <Skeleton className="mt-1 h-6 w-10" /> : <p className="text-xl font-semibold tabular-nums">{value}</p>}
      </div>
    </Card>
  );
}

interface LeadStatsBarProps {
  totalElements: number;
  hotCount: number;
  atRiskCount: number;
  avgScore: number;
  loading?: boolean;
}

/** Auto-loads with the Leads page — total/avg score come from the current
 *  page query, hot/at-risk reuse leadsApi.atRisk() which already existed on
 *  the backend but wasn't surfaced anywhere in the UI. */
export function LeadStatsBar({ totalElements, hotCount, atRiskCount, avgScore, loading }: LeadStatsBarProps) {
  return (
    <div className="mb-4 grid grid-cols-2 gap-3 sm:grid-cols-4">
      <Stat label="Total leads" value={totalElements} icon={Users2} tone="primary" loading={loading} />
      <Stat label="Hot leads" value={hotCount} icon={Flame} tone="hot" loading={loading} />
      <Stat label="At risk" value={atRiskCount} icon={AlertTriangle} tone="risk" loading={loading} />
      <Stat label="Avg. score" value={avgScore} icon={Gauge} tone="neutral" loading={loading} />
    </div>
  );
}

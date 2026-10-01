import { Sparkles, Flame, Snowflake, AlertTriangle } from "lucide-react";
import { cn } from "@/lib/utils";
import { CONTACT_ENGAGEMENT_LABELS, type ContactEngagement } from "@/types/contact";

const ENGAGEMENT_STYLE: Record<ContactEngagement, string> = {
  NEW: "bg-sky-100 text-sky-700",
  ACTIVE: "bg-emerald-100 text-emerald-800",
  COOLING: "bg-amber-100 text-amber-800",
  AT_RISK: "bg-red-100 text-red-700",
};

const ENGAGEMENT_ICON: Record<ContactEngagement, typeof Sparkles> = {
  NEW: Sparkles,
  ACTIVE: Flame,
  COOLING: Snowflake,
  AT_RISK: AlertTriangle,
};

export function EngagementBadge({ engagement }: { engagement: ContactEngagement }) {
  const Icon = ENGAGEMENT_ICON[engagement];
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium",
        ENGAGEMENT_STYLE[engagement],
      )}
    >
      <Icon className="h-3 w-3" />
      {CONTACT_ENGAGEMENT_LABELS[engagement]}
    </span>
  );
}

/** 0-100 engagement score, colored by band — same visual language as
 * leads.ScoreBadge so the two modules feel like one product. */
export function EngagementScore({ score }: { score: number }) {
  const color =
    score >= 75
      ? "text-emerald-700 bg-emerald-50"
      : score >= 45
        ? "text-amber-700 bg-amber-50"
        : "text-red-700 bg-red-50";
  return (
    <span
      className={cn(
        "inline-flex h-6 min-w-6 items-center justify-center rounded-full px-1.5 text-xs font-semibold",
        color,
      )}
    >
      {score}
    </span>
  );
}

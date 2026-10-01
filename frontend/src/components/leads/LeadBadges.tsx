import { Dot } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import { LEAD_STAGE_LABELS, type LeadStage, type Priority, type BuyingIntent } from "@/types/lead";

const STAGE_DOT_CLASS: Record<LeadStage, string> = {
  NEW: "bg-stage-new",
  CONTACTED: "bg-stage-contacted",
  QUALIFIED: "bg-stage-qualified",
  MEETING: "bg-stage-meeting",
  OPPORTUNITY: "bg-stage-opportunity",
  QUOTATION: "bg-stage-quotation",
  WON: "bg-stage-won",
  LOST: "bg-stage-lost",
};

export function StageBadge({ stage }: { stage: LeadStage }) {
  return (
    <span className="inline-flex items-center gap-1.5 text-sm font-medium">
      <Dot className={STAGE_DOT_CLASS[stage]} />
      {LEAD_STAGE_LABELS[stage]}
    </span>
  );
}

const PRIORITY_CLASS: Record<Priority, string> = {
  LOW: "text-muted-foreground",
  MEDIUM: "text-foreground",
  HIGH: "text-amber-700",
  URGENT: "text-destructive font-semibold",
};

export function PriorityBadge({ priority }: { priority: Priority }) {
  return <span className={cn("text-sm", PRIORITY_CLASS[priority])}>{priority[0] + priority.slice(1).toLowerCase()}</span>;
}

const INTENT_CLASS: Record<BuyingIntent, string> = {
  COLD: "bg-sky-100 text-sky-700",
  COOL: "bg-cyan-100 text-cyan-700",
  WARM: "bg-amber-100 text-amber-800",
  HOT: "bg-red-100 text-red-700",
};

export function BuyingIntentBadge({ intent }: { intent: BuyingIntent }) {
  return (
    <span className={cn("rounded-full px-2 py-0.5 text-xs font-medium", INTENT_CLASS[intent])}>
      {intent[0] + intent.slice(1).toLowerCase()}
    </span>
  );
}

/** 0-100 score, colored by band so a scanning eye catches hot leads immediately. */
export function ScoreBadge({ score }: { score: number }) {
  const color =
    score >= 80
      ? "text-emerald-700 bg-emerald-50"
      : score >= 50
        ? "text-amber-700 bg-amber-50"
        : "text-muted-foreground bg-muted";
  return (
    <span className={cn("inline-flex h-6 min-w-6 items-center justify-center rounded-full px-1.5 text-xs font-semibold", color)}>
      {score}
    </span>
  );
}

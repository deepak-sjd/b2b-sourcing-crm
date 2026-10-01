import { cn } from "@/lib/utils";
import { LEAD_STAGES, LEAD_STAGE_LABELS, type LeadStage } from "@/types/lead";

/**
 * Pure-SVG donut - no chart library dependency. Segment colors are read
 * straight from the --stage-* CSS variables in index.css so this stays in
 * sync with StageBadge/LeadBadges and any future per-client retint.
 */
const STAGE_VAR: Record<LeadStage, string> = {
  NEW: "--stage-new",
  CONTACTED: "--stage-contacted",
  QUALIFIED: "--stage-qualified",
  MEETING: "--stage-meeting",
  OPPORTUNITY: "--stage-opportunity",
  QUOTATION: "--stage-quotation",
  WON: "--stage-won",
  LOST: "--stage-lost",
};

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

const SIZE = 176;
const RADIUS = 68;
const STROKE = 20;
const CIRCUMFERENCE = 2 * Math.PI * RADIUS;

interface StageDonutChartProps {
  counts: Partial<Record<LeadStage, number>>;
  className?: string;
}

export function StageDonutChart({ counts, className }: StageDonutChartProps) {
  const total = LEAD_STAGES.reduce((sum, stage) => sum + (counts[stage] ?? 0), 0);

  let cumulative = 0;
  const arcs = LEAD_STAGES.map((stage) => {
    const value = counts[stage] ?? 0;
    const fraction = total > 0 ? value / total : 0;
    const dash = fraction * CIRCUMFERENCE;
    const offset = -cumulative;
    cumulative += dash;
    return { stage, value, fraction, dash, offset };
  });

  return (
    <div className={cn("flex flex-col gap-5 sm:flex-row sm:items-center", className)}>
      <div className="relative mx-auto h-[176px] w-[176px] shrink-0">
        <svg width={SIZE} height={SIZE} viewBox={`0 0 ${SIZE} ${SIZE}`} className="-rotate-90">
          <circle
            cx={SIZE / 2}
            cy={SIZE / 2}
            r={RADIUS}
            fill="none"
            stroke="hsl(var(--muted))"
            strokeWidth={STROKE}
          />
          {total > 0 &&
            arcs
              .filter((arc) => arc.value > 0)
              .map((arc) => (
                <circle
                  key={arc.stage}
                  cx={SIZE / 2}
                  cy={SIZE / 2}
                  r={RADIUS}
                  fill="none"
                  stroke={`hsl(var(${STAGE_VAR[arc.stage]}))`}
                  strokeWidth={STROKE}
                  strokeDasharray={`${arc.dash} ${CIRCUMFERENCE - arc.dash}`}
                  strokeDashoffset={arc.offset}
                  className="transition-[stroke-dasharray] duration-300"
                />
              ))}
        </svg>
        <div className="absolute inset-0 flex flex-col items-center justify-center">
          <span className="text-2xl font-semibold leading-none">{total}</span>
          <span className="mt-1 text-xs text-muted-foreground">total leads</span>
        </div>
      </div>

      <div className="flex-1 space-y-1">
        {LEAD_STAGES.map((stage) => {
          const value = counts[stage] ?? 0;
          const pct = total > 0 ? Math.round((value / total) * 100) : 0;
          return (
            <div
              key={stage}
              className="flex items-center justify-between gap-2 rounded-md px-2 py-1.5 text-sm hover:bg-muted/50"
            >
              <span className="flex items-center gap-2 text-foreground">
                <span className={cn("h-2 w-2 shrink-0 rounded-full", STAGE_DOT_CLASS[stage])} />
                {LEAD_STAGE_LABELS[stage]}
              </span>
              <span className="flex items-center gap-2 tabular-nums text-muted-foreground">
                <span className="font-medium text-foreground">{value}</span>
                <span className="w-9 text-right text-xs">{pct}%</span>
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}

import { cn } from "@/lib/utils";

interface ScoreGaugeProps {
  score: number;
  size?: number;
  strokeWidth?: number;
  className?: string;
}

/** 0-100 lead score as a circular progress ring, banded the same way as
 * ScoreBadge (emerald ≥80, amber ≥50, muted below) so the two never disagree. */
export function ScoreGauge({ score, size = 64, strokeWidth = 6, className }: ScoreGaugeProps) {
  const clamped = Math.max(0, Math.min(100, score));
  const radius = (size - strokeWidth) / 2;
  const circumference = 2 * Math.PI * radius;
  const offset = circumference * (1 - clamped / 100);

  const colorClass = clamped >= 80 ? "stroke-emerald-500" : clamped >= 50 ? "stroke-amber-500" : "stroke-muted-foreground/50";

  return (
    <div className={cn("relative inline-flex shrink-0 items-center justify-center", className)} style={{ width: size, height: size }}>
      <svg width={size} height={size} className="-rotate-90">
        <circle cx={size / 2} cy={size / 2} r={radius} strokeWidth={strokeWidth} className="fill-none stroke-muted" />
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          strokeWidth={strokeWidth}
          strokeLinecap="round"
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          className={cn("fill-none transition-[stroke-dashoffset] duration-500 ease-out", colorClass)}
        />
      </svg>
      <span className="absolute text-sm font-semibold tabular-nums">{clamped}</span>
    </div>
  );
}

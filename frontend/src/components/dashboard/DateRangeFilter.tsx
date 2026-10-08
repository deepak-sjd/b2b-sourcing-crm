import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import type { ReportDateRange } from "@/types/report";

const PRESETS: { label: string; days: number }[] = [
  { label: "7d", days: 7 },
  { label: "30d", days: 30 },
  { label: "90d", days: 90 },
];

function isoDaysAgo(days: number): string {
  const d = new Date();
  d.setDate(d.getDate() - days);
  return d.toISOString().slice(0, 10);
}

export function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

export function defaultDateRange(): Required<ReportDateRange> {
  return { from: isoDaysAgo(30), to: todayIso() };
}

interface DateRangeFilterProps {
  value: Required<ReportDateRange>;
  onChange: (range: Required<ReportDateRange>) => void;
}

/** Shared by DashboardPage and ReportsPage — every PRD §15 report endpoint takes the same from/to window. */
export function DateRangeFilter({ value, onChange }: DateRangeFilterProps) {
  return (
    <div className="flex flex-wrap items-center gap-2">
      <div className="flex gap-1">
        {PRESETS.map((preset) => (
          <Button
            key={preset.label}
            type="button"
            variant="outline"
            size="sm"
            className="h-8 px-2.5 text-xs"
            onClick={() => onChange({ from: isoDaysAgo(preset.days), to: todayIso() })}
          >
            {preset.label}
          </Button>
        ))}
      </div>
      <div className="flex items-center gap-1.5">
        <Input
          type="date"
          value={value.from}
          max={value.to}
          className="h-8 w-[142px] text-xs"
          onChange={(e) => onChange({ from: e.target.value, to: value.to })}
        />
        <span className="text-xs text-muted-foreground">to</span>
        <Input
          type="date"
          value={value.to}
          min={value.from}
          max={todayIso()}
          className="h-8 w-[142px] text-xs"
          onChange={(e) => onChange({ from: value.from, to: e.target.value })}
        />
      </div>
    </div>
  );
}

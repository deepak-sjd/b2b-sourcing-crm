import { cn } from "@/lib/utils";

export interface BarListItem {
  key: string;
  label: string;
  value: number; // drives the bar width
  secondary?: string; // shown at the right, e.g. a formatted amount
  barClassName?: string;
}

interface BarListProps {
  items: BarListItem[];
  className?: string;
}

/** A plain CSS horizontal bar list — used anywhere the spec calls for a "chart"
 *  but a real charting library would be overkill (and isn't available: no
 *  recharts/chart.js in package.json, and we're not allowed to add one). */
export function BarList({ items, className }: BarListProps) {
  const max = Math.max(1, ...items.map((i) => i.value));

  return (
    <div className={cn("space-y-3", className)}>
      {items.map((item) => (
        <div key={item.key}>
          <div className="mb-1 flex items-center justify-between gap-2 text-sm">
            <span className="truncate font-medium">{item.label}</span>
            <span className="shrink-0 tabular-nums text-muted-foreground">
              {item.value.toLocaleString("en-IN")}
              {item.secondary ? ` · ${item.secondary}` : ""}
            </span>
          </div>
          <div className="h-1.5 w-full overflow-hidden rounded-full bg-muted">
            <div
              className={cn("h-full rounded-full transition-[width] duration-300", item.barClassName ?? "bg-primary")}
              style={{ width: `${Math.max(2, (item.value / max) * 100)}%` }}
            />
          </div>
        </div>
      ))}
    </div>
  );
}

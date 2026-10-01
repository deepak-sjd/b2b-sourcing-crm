import { Dot } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import {
  SUPPLIER_CATEGORY_LABELS,
  SUPPLIER_STATUS_LABELS,
  type SupplierCategory,
  type SupplierStatus,
} from "@/types/supplier";

const STATUS_DOT_CLASS: Record<SupplierStatus, string> = {
  PENDING_VERIFICATION: "bg-amber-500",
  VERIFIED: "bg-emerald-500",
  ON_HOLD: "bg-slate-400",
  BLACKLISTED: "bg-red-500",
};

const STATUS_TEXT_CLASS: Record<SupplierStatus, string> = {
  PENDING_VERIFICATION: "text-amber-700",
  VERIFIED: "text-emerald-700",
  ON_HOLD: "text-slate-500",
  BLACKLISTED: "text-red-700",
};

export function SupplierStatusBadge({ status }: { status: SupplierStatus }) {
  return (
    <span className={cn("inline-flex items-center gap-1.5 text-sm font-medium", STATUS_TEXT_CLASS[status])}>
      <Dot className={STATUS_DOT_CLASS[status]} />
      {SUPPLIER_STATUS_LABELS[status]}
    </span>
  );
}

export function SupplierCategoryBadge({ category }: { category: SupplierCategory }) {
  return (
    <span className="inline-flex items-center rounded-full border border-border bg-secondary px-2.5 py-0.5 text-xs font-medium text-secondary-foreground">
      {SUPPLIER_CATEGORY_LABELS[category]}
    </span>
  );
}

/** 0-100 profile-strength bar, colored by band — same "scan and know instantly" idea as Lead's ScoreBadge. */
export function TrustScoreBar({ score }: { score: number }) {
  const barColor = score >= 80 ? "bg-emerald-500" : score >= 50 ? "bg-amber-500" : "bg-slate-300";
  const textColor = score >= 80 ? "text-emerald-700" : score >= 50 ? "text-amber-700" : "text-muted-foreground";

  return (
    <div className="flex w-28 items-center gap-2">
      <div className="h-1.5 flex-1 overflow-hidden rounded-full bg-muted">
        <div className={cn("h-full rounded-full transition-all", barColor)} style={{ width: `${score}%` }} />
      </div>
      <span className={cn("w-8 shrink-0 text-right text-xs font-semibold tabular-nums", textColor)}>{score}%</span>
    </div>
  );
}

import { Dot } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import { QUOTATION_STATUS_LABELS, type QuotationStatus } from "@/types/quotation";

const DOT_CLASS: Record<QuotationStatus, string> = {
  DRAFT: "bg-slate-400",
  UNDER_REVIEW: "bg-amber-500",
  SENT: "bg-blue-500",
  VIEWED: "bg-indigo-500",
  NEGOTIATION: "bg-purple-500",
  ACCEPTED: "bg-emerald-500",
  REJECTED: "bg-red-500",
  EXPIRED: "bg-slate-400",
};

const TEXT_CLASS: Record<QuotationStatus, string> = {
  DRAFT: "text-slate-500",
  UNDER_REVIEW: "text-amber-700",
  SENT: "text-blue-700",
  VIEWED: "text-indigo-700",
  NEGOTIATION: "text-purple-700",
  ACCEPTED: "text-emerald-700",
  REJECTED: "text-red-700",
  EXPIRED: "text-slate-500",
};

export function QuotationStatusBadge({ status }: { status: QuotationStatus }) {
  return (
    <span className={cn("inline-flex items-center gap-1.5 text-sm font-medium", TEXT_CLASS[status])}>
      <Dot className={DOT_CLASS[status]} />
      {QUOTATION_STATUS_LABELS[status]}
    </span>
  );
}

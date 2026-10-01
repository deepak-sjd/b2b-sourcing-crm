import { Truck } from "lucide-react";
import { cn } from "@/lib/utils";
import { REQUIREMENT_STATUS_LABELS, type RequirementStatus } from "@/types/requirement";

const STATUS_CLASS: Record<RequirementStatus, string> = {
  OPEN: "bg-sky-100 text-sky-700",
  SOURCING: "bg-amber-100 text-amber-800",
  QUOTED: "bg-violet-100 text-violet-700",
  CLOSED: "bg-emerald-100 text-emerald-700",
  CANCELLED: "bg-muted text-muted-foreground",
};

export function RequirementStatusBadge({ status }: { status: RequirementStatus }) {
  return (
    <span className={cn("rounded-full px-2 py-0.5 text-xs font-medium", STATUS_CLASS[status])}>
      {REQUIREMENT_STATUS_LABELS[status]}
    </span>
  );
}

export function NeedsSupplierBadge() {
  return (
    <span className="inline-flex items-center gap-1 rounded-full bg-orange-100 px-2 py-0.5 text-xs font-medium text-orange-700">
      <Truck className="h-3 w-3" />
      Needs supplier
    </span>
  );
}

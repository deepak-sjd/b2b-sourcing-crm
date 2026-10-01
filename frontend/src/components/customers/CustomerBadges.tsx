import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import {
  APPROVAL_STATUS_LABELS,
  NATURE_OF_BUSINESS_LABELS,
  type ApprovalStatus,
  type NatureOfBusiness,
} from "@/types/customer";

const APPROVAL_VARIANT: Record<ApprovalStatus, "success" | "warning" | "destructive"> = {
  APPROVED: "success",
  PENDING: "warning",
  REJECTED: "destructive",
};

export function ApprovalStatusBadge({ status }: { status: ApprovalStatus }) {
  return <Badge variant={APPROVAL_VARIANT[status]}>{APPROVAL_STATUS_LABELS[status]}</Badge>;
}

export function NatureOfBusinessBadge({ nature }: { nature: NatureOfBusiness | null }) {
  if (!nature) return <span className="text-sm text-muted-foreground">—</span>;
  return (
    <span className={cn("rounded-full bg-muted px-2 py-0.5 text-xs font-medium text-muted-foreground")}>
      {NATURE_OF_BUSINESS_LABELS[nature]}
    </span>
  );
}

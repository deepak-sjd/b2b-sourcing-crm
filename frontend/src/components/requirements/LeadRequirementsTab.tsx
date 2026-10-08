import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { ClipboardList, Pencil, Plus, Truck } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { EmptyState } from "@/components/common/EmptyState";
import { PriorityBadge } from "@/components/leads/LeadBadges";
import { NeedsSupplierBadge, RequirementStatusBadge } from "@/components/requirements/RequirementBadges";
import { RequirementFormDialog } from "@/components/requirements/RequirementFormDialog";
import { RequirementSourcingDialog } from "@/components/enquiries/RequirementSourcingDialog";
import { requirementsApi } from "@/api/requirements";
import { extractErrorMessage } from "@/api/client";
import { formatCurrencyINR, formatDate } from "@/lib/utils";
import {
  LOCKED_REQUIREMENT_STATUSES,
  REQUIREMENT_STATUSES,
  REQUIREMENT_STATUS_LABELS,
  type Requirement,
  type RequirementStatus,
} from "@/types/requirement";

function Detail({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div>
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="text-sm">{value || "—"}</p>
    </div>
  );
}

export function LeadRequirementsTab({ leadId }: { leadId: string }) {
  const queryClient = useQueryClient();
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<Requirement | null>(null);
  const [sourcingFor, setSourcingFor] = useState<Requirement | null>(null);

  const query = useQuery({
    queryKey: ["requirements", "lead", leadId],
    queryFn: () => requirementsApi.byLead(leadId),
  });

  const changeStatus = useMutation({
    mutationFn: ({ id, status }: { id: string; status: RequirementStatus }) =>
      requirementsApi.changeStatus(id, status),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["requirements"] }),
    onError: (error) => toast.error(extractErrorMessage(error, "Couldn't change status")),
  });

  function openNew() {
    setEditing(null);
    setFormOpen(true);
  }
  function openEdit(r: Requirement) {
    setEditing(r);
    setFormOpen(true);
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <p className="text-sm text-muted-foreground">
          What this customer needs. One customer can have several requirements.
        </p>
        <Button size="sm" onClick={openNew}>
          <Plus className="h-4 w-4" />
          New requirement
        </Button>
      </div>

      {query.isLoading ? (
        <div className="space-y-2">
          <Skeleton className="h-24 w-full" />
          <Skeleton className="h-24 w-full" />
        </div>
      ) : query.data && query.data.content.length > 0 ? (
        <div className="space-y-3">
          {query.data.content.map((r) => {
            const locked = LOCKED_REQUIREMENT_STATUSES.includes(r.status);
            return (
              <div key={r.id} className="rounded-lg border border-border p-4">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="min-w-0 space-y-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <p className="text-sm font-semibold">{r.productName}</p>
                      <RequirementStatusBadge status={r.status} />
                      {r.needsSupplier && <NeedsSupplierBadge />}
                    </div>
                    {r.description && <p className="text-sm text-muted-foreground">{r.description}</p>}
                  </div>
                  <div className="flex shrink-0 items-center gap-2">
                    <Select
                      value={r.status}
                      disabled={changeStatus.isPending}
                      onValueChange={(v) => changeStatus.mutate({ id: r.id, status: v as RequirementStatus })}
                    >
                      <SelectTrigger className="h-8 w-36">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        {REQUIREMENT_STATUSES.map((s) => (
                          <SelectItem key={s} value={s}>
                            {REQUIREMENT_STATUS_LABELS[s]}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <Button variant="outline" size="sm" onClick={() => setSourcingFor(r)}>
                      <Truck className="h-3.5 w-3.5" />
                      Sourcing
                    </Button>
                    <Button variant="outline" size="sm" disabled={locked} onClick={() => openEdit(r)}>
                      <Pencil className="h-3.5 w-3.5" />
                      Edit
                    </Button>
                  </div>
                </div>

                <div className="mt-3 grid grid-cols-2 gap-x-6 gap-y-3 sm:grid-cols-4">
                  <Detail label="Quantity" value={r.quantity != null ? `${r.quantity}${r.unit ? " " + r.unit : ""}` : null} />
                  <Detail label="Budget" value={r.budget != null ? formatCurrencyINR(r.budget) : null} />
                  <Detail label="Priority" value={<PriorityBadge priority={r.priority} />} />
                  <Detail label="Assigned to" value={r.assignedToName} />
                  <Detail label="Timeline" value={r.expectedTimeline} />
                  <Detail label="Delivery by" value={r.requiredDeliveryDate ? formatDate(r.requiredDeliveryDate) : null} />
                </div>

                {(r.additionalSpecifications || r.remarks) && (
                  <div className="mt-3 grid gap-3 border-t border-border pt-3 sm:grid-cols-2">
                    {r.additionalSpecifications && <Detail label="Additional specifications" value={r.additionalSpecifications} />}
                    {r.remarks && <Detail label="Remarks" value={r.remarks} />}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      ) : (
        <EmptyState
          icon={ClipboardList}
          title="No requirements yet"
          description="Record what the customer needs — product, quantity, budget and delivery date."
          action={
            <Button size="sm" onClick={openNew}>
              <Plus className="h-4 w-4" />
              New requirement
            </Button>
          }
        />
      )}

      <RequirementFormDialog open={formOpen} onOpenChange={setFormOpen} leadId={leadId} requirement={editing} />
      {sourcingFor && (
        <RequirementSourcingDialog
          requirement={sourcingFor}
          open={!!sourcingFor}
          onOpenChange={(open) => !open && setSourcingFor(null)}
        />
      )}
    </div>
  );
}

import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { requirementsApi } from "@/api/requirements";
import { usersApi } from "@/api/users";
import { extractErrorMessage } from "@/api/client";
import { useAuthStore } from "@/store/authStore";
import { PRIORITIES, type Priority } from "@/types/lead";
import type { Requirement, RequirementRequest } from "@/types/requirement";

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** Required when creating from a lead; ignored when editing. */
  leadId?: string;
  /** Pass to edit an existing requirement. */
  requirement?: Requirement | null;
}

const UNASSIGNED = "__default__";

interface FormState {
  productName: string;
  description: string;
  quantity: string;
  unit: string;
  expectedTimeline: string;
  budget: string;
  priority: Priority;
  requiredDeliveryDate: string;
  additionalSpecifications: string;
  remarks: string;
  needsSupplier: boolean;
  assignedToId: string;
}

const EMPTY: FormState = {
  productName: "",
  description: "",
  quantity: "",
  unit: "",
  expectedTimeline: "",
  budget: "",
  priority: "MEDIUM",
  requiredDeliveryDate: "",
  additionalSpecifications: "",
  remarks: "",
  needsSupplier: false,
  assignedToId: UNASSIGNED,
};

function fromRequirement(r: Requirement): FormState {
  return {
    productName: r.productName,
    description: r.description ?? "",
    quantity: r.quantity != null ? String(r.quantity) : "",
    unit: r.unit ?? "",
    expectedTimeline: r.expectedTimeline ?? "",
    budget: r.budget != null ? String(r.budget) : "",
    priority: r.priority,
    requiredDeliveryDate: r.requiredDeliveryDate ?? "",
    additionalSpecifications: r.additionalSpecifications ?? "",
    remarks: r.remarks ?? "",
    needsSupplier: r.needsSupplier,
    assignedToId: r.assignedToId ?? UNASSIGNED,
  };
}

function toNumber(v: string): number | null {
  if (!v.trim()) return null;
  const n = Number(v);
  return Number.isFinite(n) ? n : null;
}

export function RequirementFormDialog({ open, onOpenChange, leadId, requirement }: Props) {
  const queryClient = useQueryClient();
  const isEdit = !!requirement;
  const [form, setForm] = useState<FormState>(EMPTY);

  // GET /users is admin-only server-side, so only admins get an assignee picker;
  // everyone else's requirement defaults to the lead owner on the backend.
  const role = useAuthStore((s) => s.user?.role);
  const canPickAssignee = role === "SUPER_ADMIN" || role === "ADMIN";
  const usersQuery = useQuery({
    queryKey: ["users", "all"],
    queryFn: usersApi.listAll,
    enabled: open && canPickAssignee,
  });

  useEffect(() => {
    if (open) setForm(requirement ? fromRequirement(requirement) : EMPTY);
  }, [open, requirement]);

  function set<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  const quantity = toNumber(form.quantity);
  const budget = toNumber(form.budget);
  const quantityInvalid = form.quantity.trim() !== "" && (quantity === null || quantity <= 0);
  const budgetInvalid = form.budget.trim() !== "" && (budget === null || budget < 0);
  const canSubmit = form.productName.trim() !== "" && !quantityInvalid && !budgetInvalid;

  const save = useMutation({
    mutationFn: () => {
      const payload: RequirementRequest = {
        leadId: isEdit ? undefined : leadId,
        productName: form.productName.trim(),
        description: form.description.trim() || null,
        quantity,
        unit: form.unit.trim() || null,
        expectedTimeline: form.expectedTimeline.trim() || null,
        budget,
        priority: form.priority,
        requiredDeliveryDate: form.requiredDeliveryDate || null,
        additionalSpecifications: form.additionalSpecifications.trim() || null,
        remarks: form.remarks.trim() || null,
        needsSupplier: form.needsSupplier,
        assignedToId: form.assignedToId === UNASSIGNED ? null : form.assignedToId,
      };
      return isEdit ? requirementsApi.update(requirement!.id, payload) : requirementsApi.create(payload);
    },
    onSuccess: () => {
      toast.success(isEdit ? "Requirement updated" : "Requirement added");
      queryClient.invalidateQueries({ queryKey: ["requirements"] });
      onOpenChange(false);
    },
    onError: (error) => toast.error(extractErrorMessage(error, "Couldn't save requirement")),
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>{isEdit ? "Edit requirement" : "New requirement"}</DialogTitle>
        </DialogHeader>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            if (canSubmit) save.mutate();
          }}
          className="space-y-4"
        >
          <div className="space-y-1.5">
            <Label htmlFor="req-product">Product / service *</Label>
            <Input
              id="req-product"
              value={form.productName}
              onChange={(e) => set("productName", e.target.value)}
              maxLength={255}
              autoFocus
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="req-desc">Requirement description</Label>
            <Textarea
              id="req-desc"
              value={form.description}
              onChange={(e) => set("description", e.target.value)}
              rows={3}
              maxLength={2000}
            />
          </div>

          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            <div className="space-y-1.5">
              <Label htmlFor="req-qty">Quantity</Label>
              <Input
                id="req-qty"
                inputMode="decimal"
                value={form.quantity}
                onChange={(e) => set("quantity", e.target.value)}
                aria-invalid={quantityInvalid}
              />
              {quantityInvalid && <p className="text-xs text-destructive">Must be greater than 0</p>}
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="req-unit">Unit</Label>
              <Input
                id="req-unit"
                value={form.unit}
                onChange={(e) => set("unit", e.target.value)}
                placeholder="pcs, kg…"
                maxLength={30}
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="req-budget">Budget (₹)</Label>
              <Input
                id="req-budget"
                inputMode="decimal"
                value={form.budget}
                onChange={(e) => set("budget", e.target.value)}
                aria-invalid={budgetInvalid}
              />
              {budgetInvalid && <p className="text-xs text-destructive">Can't be negative</p>}
            </div>
            <div className="space-y-1.5">
              <Label>Priority</Label>
              <Select value={form.priority} onValueChange={(v) => set("priority", v as Priority)}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {PRIORITIES.map((p) => (
                    <SelectItem key={p} value={p}>
                      {p[0] + p.slice(1).toLowerCase()}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div className="space-y-1.5">
              <Label htmlFor="req-timeline">Expected timeline</Label>
              <Input
                id="req-timeline"
                value={form.expectedTimeline}
                onChange={(e) => set("expectedTimeline", e.target.value)}
                placeholder="e.g. within 3 weeks"
                maxLength={120}
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="req-delivery">Required delivery date</Label>
              <Input
                id="req-delivery"
                type="date"
                value={form.requiredDeliveryDate}
                onChange={(e) => set("requiredDeliveryDate", e.target.value)}
              />
            </div>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="req-specs">Additional specifications</Label>
            <Textarea
              id="req-specs"
              value={form.additionalSpecifications}
              onChange={(e) => set("additionalSpecifications", e.target.value)}
              rows={2}
              maxLength={2000}
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="req-remarks">Remarks</Label>
            <Textarea
              id="req-remarks"
              value={form.remarks}
              onChange={(e) => set("remarks", e.target.value)}
              rows={2}
              maxLength={2000}
            />
          </div>

          {canPickAssignee && (
            <div className="space-y-1.5">
              <Label>Assigned employee</Label>
              <Select value={form.assignedToId} onValueChange={(v) => set("assignedToId", v)}>
                <SelectTrigger>
                  <SelectValue placeholder={usersQuery.isLoading ? "Loading users…" : "Lead owner (default)"} />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={UNASSIGNED}>Lead owner (default)</SelectItem>
                  {usersQuery.data
                    ?.filter((u) => u.active)
                    .map((u) => (
                      <SelectItem key={u.id} value={u.id}>
                        {u.fullName}
                      </SelectItem>
                    ))}
                </SelectContent>
              </Select>
            </div>
          )}

          <label className="flex cursor-pointer items-start gap-2.5 rounded-md border border-border p-3">
            <input
              type="checkbox"
              className="mt-0.5 h-4 w-4 accent-[hsl(var(--primary))]"
              checked={form.needsSupplier}
              onChange={(e) => set("needsSupplier", e.target.checked)}
            />
            <span>
              <span className="block text-sm font-medium">Item not available — needs a supplier</span>
              <span className="block text-xs text-muted-foreground">
                Flags this requirement for supplier sourcing. You can then send an enquiry and use the supplier's
                price in the quotation.
              </span>
            </span>
          </label>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={!canSubmit || save.isPending}>
              {save.isPending ? "Saving…" : isEdit ? "Save changes" : "Add requirement"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

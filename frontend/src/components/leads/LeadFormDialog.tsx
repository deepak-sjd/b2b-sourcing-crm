import { useEffect, useState } from "react";
import { toast } from "sonner";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { extractErrorMessage } from "@/api/client";
import { leadsApi } from "@/api/leads";
import { validate, leadSchema } from "@/lib/validation";
import { PRIORITIES, type LeadRequest, type LeadResponse, type Priority } from "@/types/lead";

interface LeadFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** Present -> edit mode; absent -> create mode. */
  lead?: LeadResponse;
  onSaved: (lead: LeadResponse) => void;
}

const emptyForm = {
  fullName: "",
  phone: "",
  email: "",
  city: "",
  interest: "",
  budget: "",
  priority: "MEDIUM" as Priority,
  source: "",
};

export function LeadFormDialog({ open, onOpenChange, lead, onSaved }: LeadFormDialogProps) {
  const [form, setForm] = useState(emptyForm);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!open) return;
    setErrors({});
    if (lead) {
      setForm({
        fullName: lead.fullName,
        phone: lead.phone ?? "",
        email: lead.email ?? "",
        city: lead.city ?? "",
        interest: lead.interest ?? "",
        budget: lead.budget != null ? String(lead.budget) : "",
        priority: lead.priority,
        source: lead.source ?? "",
      });
    } else {
      setForm(emptyForm);
    }
  }, [open, lead]);

  function update<K extends keyof typeof form>(key: K, value: (typeof form)[K]) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const budgetNum = form.budget.trim() === "" ? undefined : Number(form.budget);
    const result = validate(leadSchema, { ...form, budget: budgetNum });
    if (!result.success) {
      setErrors(result.errors);
      return;
    }
    setErrors({});

    const payload: LeadRequest = {
      fullName: form.fullName.trim(),
      phone: form.phone.trim() || null,
      email: form.email.trim() || null,
      city: form.city.trim() || null,
      interest: form.interest.trim() || null,
      budget: budgetNum ?? null,
      priority: form.priority,
      source: form.source.trim() || null,
    };

    setSubmitting(true);
    try {
      const saved = lead ? await leadsApi.update(lead.id, payload) : await leadsApi.create(payload);
      toast.success(lead ? "Lead updated" : "Lead created");
      onSaved(saved);
      onOpenChange(false);
    } catch (error) {
      toast.error(extractErrorMessage(error, "Couldn't save this lead"));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-xl">
        <DialogHeader>
          <DialogTitle>{lead ? "Edit lead" : "New lead"}</DialogTitle>
          <DialogDescription>
            {lead ? "Update this lead's details." : "Add a new lead to the pipeline."}
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="max-h-[70vh] space-y-5 overflow-y-auto pr-1 thin-scrollbar" noValidate>
          <section className="space-y-3">
            <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">Contact information</p>
            <div className="grid grid-cols-2 gap-4">
              <div className="col-span-2 space-y-1.5">
                <Label htmlFor="fullName">Full name</Label>
                <Input id="fullName" value={form.fullName} onChange={(e) => update("fullName", e.target.value)} />
                {errors.fullName && <p className="text-xs text-destructive">{errors.fullName}</p>}
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="phone">Phone</Label>
                <Input id="phone" value={form.phone} onChange={(e) => update("phone", e.target.value)} />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="email">Email</Label>
                <Input id="email" type="email" value={form.email} onChange={(e) => update("email", e.target.value)} />
                {errors.email && <p className="text-xs text-destructive">{errors.email}</p>}
              </div>
              <div className="col-span-2 space-y-1.5">
                <Label htmlFor="city">City</Label>
                <Input id="city" value={form.city} onChange={(e) => update("city", e.target.value)} />
              </div>
            </div>
          </section>

          <section className="space-y-3 border-t border-border pt-4">
            <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">Deal details</p>
            <div className="grid grid-cols-2 gap-4">
              <div className="col-span-2 space-y-1.5">
                <Label htmlFor="interest">Interest</Label>
                <Input
                  id="interest"
                  placeholder="e.g. Premium Plan"
                  value={form.interest}
                  onChange={(e) => update("interest", e.target.value)}
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="budget">Budget (₹)</Label>
                <Input
                  id="budget"
                  type="number"
                  min="0"
                  value={form.budget}
                  onChange={(e) => update("budget", e.target.value)}
                />
              </div>
              <div className="space-y-1.5">
                <Label>Priority</Label>
                <Select value={form.priority} onValueChange={(v) => update("priority", v as Priority)}>
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
              <div className="col-span-2 space-y-1.5">
                <Label htmlFor="source">Source</Label>
                <Input
                  id="source"
                  placeholder="e.g. Meta Ads, Referral, Organic"
                  value={form.source}
                  onChange={(e) => update("source", e.target.value)}
                />
              </div>
            </div>
          </section>

          {!lead && (
            <p className="rounded-md bg-accent px-3 py-2 text-xs text-accent-foreground">
              Score, buying intent, and the AI summary are generated automatically once this lead is created —
              nothing to fill in here.
            </p>
          )}

          <DialogFooter className="border-t border-border pt-4">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={submitting}>
              {submitting ? "Saving…" : lead ? "Save changes" : "Create lead"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

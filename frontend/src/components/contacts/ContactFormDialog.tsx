import { useEffect, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { toast } from "sonner";
import { AlertTriangle } from "lucide-react";
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
import { extractErrorMessage } from "@/api/client";
import { contactsApi } from "@/api/contacts";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import { validate, contactSchema } from "@/lib/validation";
import type { ContactRequest, ContactResponse } from "@/types/contact";

interface ContactFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** Present -> edit mode; absent -> create mode. */
  contact?: ContactResponse;
  onSaved: (contact: ContactResponse) => void;
}

const emptyForm = {
  fullName: "",
  designation: "",
  company: "",
  phone: "",
  email: "",
  city: "",
  source: "",
};

export function ContactFormDialog({ open, onOpenChange, contact, onSaved }: ContactFormDialogProps) {
  const [form, setForm] = useState(emptyForm);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);
  const [dismissedDuplicate, setDismissedDuplicate] = useState(false);

  useEffect(() => {
    if (!open) return;
    setErrors({});
    setDismissedDuplicate(false);
    if (contact) {
      setForm({
        fullName: contact.fullName,
        designation: contact.designation ?? "",
        company: contact.company ?? "",
        phone: contact.phone ?? "",
        email: contact.email ?? "",
        city: contact.city ?? "",
        source: contact.source ?? "",
      });
    } else {
      setForm(emptyForm);
    }
  }, [open, contact]);

  function update<K extends keyof typeof form>(key: K, value: (typeof form)[K]) {
    setForm((prev) => ({ ...prev, [key]: value }));
    setDismissedDuplicate(false);
  }

  const debouncedPhone = useDebouncedValue(form.phone, 400);
  const debouncedEmail = useDebouncedValue(form.email, 400);

  // AI-assist: as soon as the counselor has typed enough of a phone or email
  // to be meaningful, check whether this person is already in the system
  // under a different owner — this is what stops the same contact quietly
  // being added twice.
  const duplicateQuery = useQuery({
    queryKey: ["contacts", "check-duplicate", debouncedPhone, debouncedEmail, contact?.id],
    queryFn: () =>
      contactsApi.checkDuplicate({
        phone: debouncedPhone.trim().length >= 6 ? debouncedPhone.trim() : undefined,
        email: debouncedEmail.includes("@") ? debouncedEmail.trim() : undefined,
        excludeId: contact?.id,
      }),
    enabled: open && (debouncedPhone.trim().length >= 6 || debouncedEmail.includes("@")),
  });

  const duplicate = !dismissedDuplicate ? duplicateQuery.data : undefined;

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const result = validate(contactSchema, form);
    if (!result.success) {
      setErrors(result.errors);
      return;
    }
    setErrors({});

    const payload: ContactRequest = {
      fullName: form.fullName.trim(),
      designation: form.designation.trim() || null,
      company: form.company.trim() || null,
      phone: form.phone.trim() || null,
      email: form.email.trim() || null,
      city: form.city.trim() || null,
      source: form.source.trim() || null,
      emailBounced: contact?.emailBounced ?? false,
    };

    setSubmitting(true);
    try {
      const saved = contact
        ? await contactsApi.update(contact.id, payload)
        : await contactsApi.create(payload);
      toast.success(contact ? "Contact updated" : "Contact created");
      onSaved(saved);
      onOpenChange(false);
    } catch (error) {
      toast.error(extractErrorMessage(error, "Couldn't save this contact"));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>{contact ? "Edit contact" : "New contact"}</DialogTitle>
          <DialogDescription>
            {contact ? "Update this contact's details." : "Add a person at a customer account."}
          </DialogDescription>
        </DialogHeader>

        {duplicate?.duplicate && duplicate.match && (
          <div className="flex items-start gap-2 rounded-md border border-amber-200 bg-amber-50 p-3 text-sm text-amber-900">
            <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" />
            <div className="flex-1">
              <p className="font-medium">Possible duplicate</p>
              <p className="text-amber-800">
                {duplicate.match.fullName}
                {duplicate.match.company ? ` at ${duplicate.match.company}` : ""} already has this{" "}
                {duplicate.matchedOn} on file{duplicate.match.ownerName ? ` (owned by ${duplicate.match.ownerName})` : ""}.
              </p>
            </div>
            <button
              type="button"
              className="text-xs font-medium text-amber-900 underline underline-offset-2"
              onClick={() => setDismissedDuplicate(true)}
            >
              Not a duplicate
            </button>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4" noValidate>
          <div className="grid grid-cols-2 gap-4">
            <div className="col-span-2 space-y-1.5">
              <Label htmlFor="fullName">Contact name</Label>
              <Input id="fullName" value={form.fullName} onChange={(e) => update("fullName", e.target.value)} />
              {errors.fullName && <p className="text-xs text-destructive">{errors.fullName}</p>}
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="designation">Designation</Label>
              <Input
                id="designation"
                placeholder="e.g. IT Manager"
                value={form.designation}
                onChange={(e) => update("designation", e.target.value)}
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="company">Company</Label>
              <Input
                id="company"
                placeholder="e.g. Telivus Systems"
                value={form.company}
                onChange={(e) => update("company", e.target.value)}
              />
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
            <div className="space-y-1.5">
              <Label htmlFor="city">City</Label>
              <Input id="city" value={form.city} onChange={(e) => update("city", e.target.value)} />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="source">Source</Label>
              <Input
                id="source"
                placeholder="e.g. Referral"
                value={form.source}
                onChange={(e) => update("source", e.target.value)}
              />
            </div>
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={submitting}>
              {submitting ? "Saving…" : contact ? "Save changes" : "Create contact"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

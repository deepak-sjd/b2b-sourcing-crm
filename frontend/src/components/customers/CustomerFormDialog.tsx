import { useEffect, useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { toast } from "sonner";
import { Sparkles } from "lucide-react";
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
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { CustomerContactsCard } from "./CustomerContactsCard";
import { DuplicateWarningBanner } from "./DuplicateWarningBanner";
import { customersApi } from "@/api/customers";
import { usersApi } from "@/api/users";
import { extractErrorMessage } from "@/api/client";
import { useAuthStore } from "@/store/authStore";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import { validate, customerSchema } from "@/lib/validation";
import {
  NATURE_OF_BUSINESS_OPTIONS,
  NATURE_OF_BUSINESS_LABELS,
  emptyAddress,
  type Address,
  type CustomerContact,
  type CustomerRequest,
  type CustomerResponse,
  type NatureOfBusiness,
} from "@/types/customer";

interface CustomerFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  customer?: CustomerResponse;
  onSaved: (customer: CustomerResponse) => void;
}

interface FormState {
  companyName: string;
  gstNo: string;
  natureOfBusiness: NatureOfBusiness | "";
  accountManagerId: string;
  billingAddress: Address;
  shippingAddress: Address;
  shippingSameAsBilling: boolean;
  contacts: CustomerContact[];
}

const emptyForm: FormState = {
  companyName: "",
  gstNo: "",
  natureOfBusiness: "",
  accountManagerId: "",
  billingAddress: { ...emptyAddress },
  shippingAddress: { ...emptyAddress },
  shippingSameAsBilling: true,
  contacts: [],
};

export function CustomerFormDialog({ open, onOpenChange, customer, onSaved }: CustomerFormDialogProps) {
  const role = useAuthStore((s) => s.user?.role);
  const canBrowseUsers = role === "SUPER_ADMIN" || role === "ADMIN" || role === "MANAGER";

  const [form, setForm] = useState<FormState>(emptyForm);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);
  const [gstStateHint, setGstStateHint] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return;
    setErrors({});
    setGstStateHint(null);
    if (customer) {
      setForm({
        companyName: customer.companyName,
        gstNo: customer.gstNo ?? "",
        natureOfBusiness: customer.natureOfBusiness ?? "",
        accountManagerId: customer.accountManagerId ?? "",
        billingAddress: { ...emptyAddress, ...customer.billingAddress },
        shippingAddress: { ...emptyAddress, ...customer.shippingAddress },
        shippingSameAsBilling: customer.shippingSameAsBilling,
        contacts: customer.contacts,
      });
    } else {
      setForm(emptyForm);
    }
  }, [open, customer]);

  const usersQuery = useQuery({
    queryKey: ["users", "all"],
    queryFn: usersApi.listAll,
    enabled: open && canBrowseUsers,
  });

  // --- AI automation #3: suggest an account manager for a brand-new customer ---
  const suggestionQuery = useQuery({
    queryKey: ["customers", "suggest-account-manager"],
    queryFn: customersApi.suggestAccountManager,
    enabled: open && !customer && canBrowseUsers,
  });

  // --- AI automation #2: GST number -> state auto-fill ---
  const debouncedGst = useDebouncedValue(form.gstNo, 400);
  useEffect(() => {
    if (!open || debouncedGst.trim().length < 2) {
      setGstStateHint(null);
      return;
    }
    let cancelled = false;
    customersApi.gstLookup(debouncedGst.trim()).then((info) => {
      if (cancelled || !info.valid || !info.state) return;
      setGstStateHint(info.state);
      // Only fills in State when it's still empty - never overwrites something
      // the user already typed themselves.
      setForm((prev) =>
        prev.billingAddress.state
          ? prev
          : { ...prev, billingAddress: { ...prev.billingAddress, state: info.state! } },
      );
    });
    return () => {
      cancelled = true;
    };
  }, [debouncedGst, open]);

  // --- AI automation #1: duplicate detection ---
  const debouncedCompanyName = useDebouncedValue(form.companyName, 400);
  const duplicateQuery = useQuery({
    queryKey: ["customers", "duplicate-check", debouncedCompanyName, form.gstNo, customer?.id],
    queryFn: () => customersApi.checkDuplicate(debouncedCompanyName.trim(), form.gstNo || undefined, customer?.id),
    enabled: open && debouncedCompanyName.trim().length >= 3,
  });

  const duplicates = useMemo(
    () => (duplicateQuery.data ?? []).filter((d) => d.id !== customer?.id),
    [duplicateQuery.data, customer],
  );

  function updateBilling(patch: Partial<Address>) {
    setForm((prev) => ({
      ...prev,
      billingAddress: { ...prev.billingAddress, ...patch },
      // Keep the (hidden) shipping copy in sync so a later "uncheck same as
      // billing" starts from what was actually entered, not stale defaults.
      shippingAddress: prev.shippingSameAsBilling ? { ...prev.shippingAddress, ...patch } : prev.shippingAddress,
    }));
  }

  function updateShipping(patch: Partial<Address>) {
    setForm((prev) => ({ ...prev, shippingAddress: { ...prev.shippingAddress, ...patch } }));
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const result = validate(customerSchema, {
      companyName: form.companyName,
      gstNo: form.gstNo,
      billingAddress: form.billingAddress,
    });
    if (!result.success) {
      setErrors(result.errors);
      return;
    }
    setErrors({});

    const payload: CustomerRequest = {
      companyName: form.companyName.trim(),
      gstNo: form.gstNo.trim() || null,
      natureOfBusiness: form.natureOfBusiness || null,
      accountManagerId: form.accountManagerId || null,
      billingAddress: form.billingAddress,
      shippingAddress: form.shippingSameAsBilling ? form.billingAddress : form.shippingAddress,
      shippingSameAsBilling: form.shippingSameAsBilling,
      contacts: form.contacts,
    };

    setSubmitting(true);
    try {
      const saved = customer ? await customersApi.update(customer.id, payload) : await customersApi.create(payload);
      toast.success(customer ? "Customer updated" : "Customer created");
      onSaved(saved);
      onOpenChange(false);
    } catch (error) {
      toast.error(extractErrorMessage(error, "Couldn't save this customer"));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>{customer ? "Edit customer" : "New customer"}</DialogTitle>
          <DialogDescription>
            {customer ? "Update this customer's details." : "Add a new customer record."}
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4" noValidate>
          <DuplicateWarningBanner matches={duplicates} />

          <Tabs defaultValue="company">
            <TabsList>
              <TabsTrigger value="company">Company</TabsTrigger>
              <TabsTrigger value="address">Address</TabsTrigger>
              <TabsTrigger value="contacts">Contacts</TabsTrigger>
            </TabsList>

            <TabsContent value="company" className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="col-span-2 space-y-1.5">
                  <Label htmlFor="companyName">Company name</Label>
                  <Input
                    id="companyName"
                    value={form.companyName}
                    onChange={(e) => setForm((p) => ({ ...p, companyName: e.target.value }))}
                  />
                  {errors.companyName && <p className="text-xs text-destructive">{errors.companyName}</p>}
                </div>
                <div className="space-y-1.5">
                  <Label htmlFor="gstNo">GST No.</Label>
                  <Input
                    id="gstNo"
                    value={form.gstNo}
                    onChange={(e) => setForm((p) => ({ ...p, gstNo: e.target.value }))}
                    placeholder="e.g. 29ABCDE1234F1Z5"
                  />
                  {gstStateHint && <p className="text-xs text-muted-foreground">Detected state: {gstStateHint}</p>}
                </div>
                <div className="space-y-1.5">
                  <Label>Nature of business</Label>
                  <Select
                    value={form.natureOfBusiness}
                    onValueChange={(v) => setForm((p) => ({ ...p, natureOfBusiness: v as NatureOfBusiness }))}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Select" />
                    </SelectTrigger>
                    <SelectContent>
                      {NATURE_OF_BUSINESS_OPTIONS.map((n) => (
                        <SelectItem key={n} value={n}>
                          {NATURE_OF_BUSINESS_LABELS[n]}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="col-span-2 space-y-1.5">
                  <div className="flex items-center justify-between">
                    <Label>Account manager</Label>
                    {suggestionQuery.data && !form.accountManagerId && (
                      <button
                        type="button"
                        onClick={() => setForm((p) => ({ ...p, accountManagerId: suggestionQuery.data!.userId }))}
                        className="inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline"
                      >
                        <Sparkles className="h-3 w-3" />
                        Suggest: {suggestionQuery.data.fullName}
                      </button>
                    )}
                  </div>
                  {canBrowseUsers ? (
                    <Select
                      value={form.accountManagerId}
                      onValueChange={(v) => setForm((p) => ({ ...p, accountManagerId: v }))}
                    >
                      <SelectTrigger>
                        <SelectValue placeholder={usersQuery.isLoading ? "Loading…" : "Select a user"} />
                      </SelectTrigger>
                      <SelectContent>
                        {usersQuery.data?.map((u) => (
                          <SelectItem key={u.id} value={u.id}>
                            {u.fullName}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  ) : (
                    <p className="text-xs text-muted-foreground">
                      Only admins and managers can reassign the account manager.
                    </p>
                  )}
                </div>
              </div>
            </TabsContent>

            <TabsContent value="address" className="space-y-5">
              <div>
                <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                  Bill to address
                </p>
                <AddressFields
                  address={form.billingAddress}
                  onChange={updateBilling}
                  errors={errors}
                  prefix="billingAddress"
                />
              </div>

              <label className="flex items-center gap-2 text-sm">
                <input
                  type="checkbox"
                  className="h-4 w-4 rounded border-input"
                  checked={form.shippingSameAsBilling}
                  onChange={(e) => setForm((p) => ({ ...p, shippingSameAsBilling: e.target.checked }))}
                />
                Shipping address same as billing
              </label>

              {!form.shippingSameAsBilling && (
                <div>
                  <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                    Ship to address
                  </p>
                  <AddressFields address={form.shippingAddress} onChange={updateShipping} errors={{}} prefix="shippingAddress" />
                </div>
              )}
            </TabsContent>

            <TabsContent value="contacts">
              <CustomerContactsCard
                contacts={form.contacts}
                onChange={(contacts) => setForm((p) => ({ ...p, contacts }))}
              />
            </TabsContent>
          </Tabs>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={submitting}>
              {submitting ? "Saving…" : customer ? "Save changes" : "Create customer"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

function AddressFields({
  address,
  onChange,
  errors,
  prefix,
}: {
  address: Address;
  onChange: (patch: Partial<Address>) => void;
  errors: Record<string, string>;
  prefix: string;
}) {
  return (
    <div className="grid grid-cols-2 gap-4">
      <div className="col-span-2 space-y-1.5">
        <Label>Address line 1</Label>
        <Input value={address.line1} onChange={(e) => onChange({ line1: e.target.value })} />
        {errors[`${prefix}.line1`] && <p className="text-xs text-destructive">{errors[`${prefix}.line1`]}</p>}
      </div>
      <div className="col-span-2 space-y-1.5">
        <Label>Address line 2</Label>
        <Input value={address.line2 ?? ""} onChange={(e) => onChange({ line2: e.target.value })} />
      </div>
      <div className="space-y-1.5">
        <Label>Area</Label>
        <Input value={address.area ?? ""} onChange={(e) => onChange({ area: e.target.value })} />
      </div>
      <div className="space-y-1.5">
        <Label>Country</Label>
        <Input value={address.country ?? ""} onChange={(e) => onChange({ country: e.target.value })} />
      </div>
      <div className="space-y-1.5">
        <Label>State</Label>
        <Input value={address.state ?? ""} onChange={(e) => onChange({ state: e.target.value })} />
      </div>
      <div className="space-y-1.5">
        <Label>City</Label>
        <Input value={address.city ?? ""} onChange={(e) => onChange({ city: e.target.value })} />
      </div>
      <div className="space-y-1.5">
        <Label>Pin code</Label>
        <Input value={address.pinCode ?? ""} onChange={(e) => onChange({ pinCode: e.target.value })} />
      </div>
      <div className="space-y-1.5">
        <Label>Contact number</Label>
        <Input value={address.phone ?? ""} onChange={(e) => onChange({ phone: e.target.value })} />
      </div>
      <div className="space-y-1.5">
        <Label>Email</Label>
        <Input type="email" value={address.email ?? ""} onChange={(e) => onChange({ email: e.target.value })} />
      </div>
      <div className="space-y-1.5">
        <Label>Billing GST No.</Label>
        <Input value={address.gstNo ?? ""} onChange={(e) => onChange({ gstNo: e.target.value })} />
      </div>
    </div>
  );
}

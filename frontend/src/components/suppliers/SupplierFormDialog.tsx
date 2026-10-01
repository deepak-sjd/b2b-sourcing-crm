import { useEffect, useState } from "react";
import { toast } from "sonner";
import { AlertTriangle, BadgeCheck, Loader2, ShieldAlert, X } from "lucide-react";
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
import { Textarea } from "@/components/ui/textarea";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { extractErrorMessage } from "@/api/client";
import { suppliersApi } from "@/api/suppliers";
import { validate, supplierSchema } from "@/lib/validation";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import {
  PAYMENT_TERMS,
  PAYMENT_TERMS_LABELS,
  SUPPLIER_CATEGORIES,
  SUPPLIER_CATEGORY_LABELS,
  type PaymentTerms,
  type SupplierCategory,
  type SupplierDuplicateMatch,
  type SupplierRequest,
  type SupplierResponse,
} from "@/types/supplier";

interface SupplierFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** Present -> edit mode; absent -> create mode. */
  supplier?: SupplierResponse;
  onSaved: (supplier: SupplierResponse) => void;
}

const emptyForm = {
  companyName: "",
  category: "OTHER" as SupplierCategory,
  gstNumber: "",
  panNumber: "",
  addressLine1: "",
  addressLine2: "",
  country: "India",
  state: "",
  city: "",
  pincode: "",
  bankName: "",
  bankAddress: "",
  accountHolderName: "",
  accountNumber: "",
  ifscCode: "",
  contactName: "",
  contactDesignation: "",
  contactEmail: "",
  contactPhone: "",
  paymentTerms: "" as PaymentTerms | "",
  products: [] as string[],
};

type FormState = typeof emptyForm;

export function SupplierFormDialog({ open, onOpenChange, supplier, onSaved }: SupplierFormDialogProps) {
  const [form, setForm] = useState<FormState>(emptyForm);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);
  const [productDraft, setProductDraft] = useState("");
  const [tab, setTab] = useState("company");

  const [gstStatus, setGstStatus] = useState<"idle" | "checking" | "valid" | "invalid">("idle");
  const [duplicates, setDuplicates] = useState<SupplierDuplicateMatch[]>([]);
  const debouncedGst = useDebouncedValue(form.gstNumber, 400);
  const debouncedName = useDebouncedValue(form.companyName, 500);
  const debouncedPhone = useDebouncedValue(form.contactPhone, 500);

  useEffect(() => {
    if (!open) return;
    setErrors({});
    setTab("company");
    setDuplicates([]);
    setGstStatus("idle");
    if (supplier) {
      setForm({
        companyName: supplier.companyName,
        category: supplier.category,
        gstNumber: supplier.gstNumber ?? "",
        panNumber: supplier.panNumber ?? "",
        addressLine1: supplier.addressLine1,
        addressLine2: supplier.addressLine2 ?? "",
        country: supplier.country || "India",
        state: supplier.state ?? "",
        city: supplier.city ?? "",
        pincode: supplier.pincode ?? "",
        bankName: supplier.bankName ?? "",
        bankAddress: supplier.bankAddress ?? "",
        accountHolderName: supplier.accountHolderName ?? "",
        accountNumber: supplier.accountNumber ?? "",
        ifscCode: supplier.ifscCode ?? "",
        contactName: supplier.contactName ?? "",
        contactDesignation: supplier.contactDesignation ?? "",
        contactEmail: supplier.contactEmail ?? "",
        contactPhone: supplier.contactPhone ?? "",
        paymentTerms: supplier.paymentTerms ?? "",
        products: supplier.products ?? [],
      });
      setGstStatus(supplier.gstVerified ? "valid" : "idle");
    } else {
      setForm(emptyForm);
    }
  }, [open, supplier]);

  // --- Smart GST auto-fill: validate structure + checksum offline and drop the
  // detected state straight into the address tab, so staff don't retype it. ---
  useEffect(() => {
    if (!open) return;
    const gstin = debouncedGst.trim().toUpperCase();
    if (gstin.length !== 15) {
      setGstStatus("idle");
      return;
    }
    let cancelled = false;
    setGstStatus("checking");
    suppliersApi
      .gstLookup(gstin)
      .then((result) => {
        if (cancelled) return;
        if (result.structureValid && result.checksumValid) {
          setGstStatus("valid");
          setForm((prev) => ({
            ...prev,
            state: prev.state.trim() ? prev.state : result.stateName ?? prev.state,
            panNumber: prev.panNumber.trim() ? prev.panNumber : result.panNumber ?? prev.panNumber,
          }));
        } else {
          setGstStatus("invalid");
        }
      })
      .catch(() => !cancelled && setGstStatus("invalid"));
    return () => {
      cancelled = true;
    };
  }, [debouncedGst, open]);

  // --- Duplicate detection: re-checks whenever name/GST/phone settle, purely advisory. ---
  useEffect(() => {
    if (!open) return;
    if (!debouncedName.trim() && !debouncedGst.trim() && !debouncedPhone?.trim()) {
      setDuplicates([]);
      return;
    }
    let cancelled = false;
    suppliersApi
      .checkDuplicates({
        companyName: debouncedName,
        gstNumber: debouncedGst,
        contactPhone: debouncedPhone,
        excludeId: supplier?.id ?? null,
      })
      .then((matches) => !cancelled && setDuplicates(matches))
      .catch(() => !cancelled && setDuplicates([]));
    return () => {
      cancelled = true;
    };
  }, [debouncedName, debouncedGst, debouncedPhone, open, supplier?.id]);

  function update<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  function addProduct() {
    const value = productDraft.trim();
    if (!value) return;
    if (!form.products.some((p) => p.toLowerCase() === value.toLowerCase())) {
      update("products", [...form.products, value]);
    }
    setProductDraft("");
  }

  function removeProduct(value: string) {
    update(
      "products",
      form.products.filter((p) => p !== value),
    );
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const result = validate(supplierSchema, form);
    if (!result.success) {
      setErrors(result.errors);
      // Jump to whichever tab holds the first invalid field.
      const key = Object.keys(result.errors)[0];
      if (["companyName", "category", "gstNumber"].includes(key)) setTab("company");
      else if (["addressLine1", "city", "state", "pincode"].includes(key)) setTab("address");
      else setTab("contact");
      return;
    }
    setErrors({});

    const payload: SupplierRequest = {
      companyName: form.companyName.trim(),
      category: form.category,
      gstNumber: form.gstNumber.trim() || null,
      panNumber: form.panNumber.trim() || null,
      addressLine1: form.addressLine1.trim(),
      addressLine2: form.addressLine2.trim() || null,
      country: form.country.trim() || "India",
      state: form.state.trim() || null,
      city: form.city.trim() || null,
      pincode: form.pincode.trim() || null,
      bankName: form.bankName.trim() || null,
      bankAddress: form.bankAddress.trim() || null,
      accountHolderName: form.accountHolderName.trim() || null,
      accountNumber: form.accountNumber.trim() || null,
      ifscCode: form.ifscCode.trim() || null,
      contactName: form.contactName.trim() || null,
      contactDesignation: form.contactDesignation.trim() || null,
      contactEmail: form.contactEmail.trim() || null,
      contactPhone: form.contactPhone.trim() || null,
      paymentTerms: form.paymentTerms || null,
      products: form.products,
    };

    setSubmitting(true);
    try {
      const saved = supplier
        ? await suppliersApi.update(supplier.id, payload)
        : await suppliersApi.create(payload);
      toast.success(supplier ? "Supplier updated" : "Supplier added");
      onSaved(saved);
      onOpenChange(false);
    } catch (error) {
      toast.error(extractErrorMessage(error, "Couldn't save this supplier"));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[88vh] max-w-2xl overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{supplier ? "Edit supplier" : "Add supplier"}</DialogTitle>
          <DialogDescription>
            {supplier ? "Update this vendor's details." : "Onboard a new vendor for procurement."}
          </DialogDescription>
        </DialogHeader>

        {duplicates.length > 0 && (
          <div className="flex items-start gap-2 rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800">
            <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" />
            <div>
              <p className="font-medium">Possible duplicate — double-check before saving</p>
              <ul className="mt-0.5 list-inside list-disc text-xs">
                {duplicates.slice(0, 3).map((d) => (
                  <li key={d.id}>{d.reason}</li>
                ))}
              </ul>
            </div>
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate>
          <Tabs value={tab} onValueChange={setTab}>
            <TabsList>
              <TabsTrigger value="company">Company & Tax</TabsTrigger>
              <TabsTrigger value="address">Address & Bank</TabsTrigger>
              <TabsTrigger value="contact">Contact & Products</TabsTrigger>
            </TabsList>

            <TabsContent value="company" className="space-y-4 pt-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="col-span-2 space-y-1.5">
                  <Label htmlFor="companyName">Company name</Label>
                  <Input
                    id="companyName"
                    value={form.companyName}
                    onChange={(e) => update("companyName", e.target.value)}
                  />
                  {errors.companyName && <p className="text-xs text-destructive">{errors.companyName}</p>}
                </div>
                <div className="space-y-1.5">
                  <Label>Category</Label>
                  <Select value={form.category} onValueChange={(v) => update("category", v as SupplierCategory)}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {SUPPLIER_CATEGORIES.map((c) => (
                        <SelectItem key={c} value={c}>
                          {SUPPLIER_CATEGORY_LABELS[c]}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-1.5">
                  <Label>Payment terms</Label>
                  <Select
                    value={form.paymentTerms}
                    onValueChange={(v) => update("paymentTerms", v as PaymentTerms)}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Select" />
                    </SelectTrigger>
                    <SelectContent>
                      {PAYMENT_TERMS.map((p) => (
                        <SelectItem key={p} value={p}>
                          {PAYMENT_TERMS_LABELS[p]}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="col-span-2 space-y-1.5">
                  <Label htmlFor="gstNumber">GST number</Label>
                  <div className="relative">
                    <Input
                      id="gstNumber"
                      placeholder="22AAAAA0000A1Z5"
                      className="pr-9 uppercase"
                      value={form.gstNumber}
                      onChange={(e) => update("gstNumber", e.target.value.toUpperCase())}
                      maxLength={15}
                    />
                    <span className="absolute right-2.5 top-1/2 -translate-y-1/2">
                      {gstStatus === "checking" && (
                        <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />
                      )}
                      {gstStatus === "valid" && <BadgeCheck className="h-4 w-4 text-emerald-600" />}
                      {gstStatus === "invalid" && <ShieldAlert className="h-4 w-4 text-destructive" />}
                    </span>
                  </div>
                  {errors.gstNumber ? (
                    <p className="text-xs text-destructive">{errors.gstNumber}</p>
                  ) : gstStatus === "valid" ? (
                    <p className="text-xs text-emerald-700">Valid GSTIN — state auto-filled below.</p>
                  ) : gstStatus === "invalid" ? (
                    <p className="text-xs text-destructive">This GSTIN's format or checksum looks wrong.</p>
                  ) : (
                    <p className="text-xs text-muted-foreground">Optional, but unlocks GST-verified status.</p>
                  )}
                </div>
                <div className="space-y-1.5">
                  <Label htmlFor="panNumber">PAN</Label>
                  <Input
                    id="panNumber"
                    className="uppercase"
                    value={form.panNumber}
                    onChange={(e) => update("panNumber", e.target.value.toUpperCase())}
                  />
                </div>
              </div>
            </TabsContent>

            <TabsContent value="address" className="space-y-4 pt-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="col-span-2 space-y-1.5">
                  <Label htmlFor="addressLine1">Address line 1</Label>
                  <Input
                    id="addressLine1"
                    value={form.addressLine1}
                    onChange={(e) => update("addressLine1", e.target.value)}
                  />
                  {errors.addressLine1 && <p className="text-xs text-destructive">{errors.addressLine1}</p>}
                </div>
                <div className="col-span-2 space-y-1.5">
                  <Label htmlFor="addressLine2">Address line 2</Label>
                  <Input
                    id="addressLine2"
                    value={form.addressLine2}
                    onChange={(e) => update("addressLine2", e.target.value)}
                  />
                </div>
                <div className="space-y-1.5">
                  <Label htmlFor="city">City</Label>
                  <Input id="city" value={form.city} onChange={(e) => update("city", e.target.value)} />
                  {errors.city && <p className="text-xs text-destructive">{errors.city}</p>}
                </div>
                <div className="space-y-1.5">
                  <Label htmlFor="state">State</Label>
                  <Input id="state" value={form.state} onChange={(e) => update("state", e.target.value)} />
                  {errors.state && <p className="text-xs text-destructive">{errors.state}</p>}
                </div>
                <div className="space-y-1.5">
                  <Label htmlFor="pincode">Pin code</Label>
                  <Input id="pincode" value={form.pincode} onChange={(e) => update("pincode", e.target.value)} />
                  {errors.pincode && <p className="text-xs text-destructive">{errors.pincode}</p>}
                </div>
                <div className="space-y-1.5">
                  <Label htmlFor="country">Country</Label>
                  <Input id="country" value={form.country} onChange={(e) => update("country", e.target.value)} />
                </div>
              </div>

              <p className="pt-1 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                Banking (optional)
              </p>
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <Label htmlFor="bankName">Bank name</Label>
                  <Input id="bankName" value={form.bankName} onChange={(e) => update("bankName", e.target.value)} />
                </div>
                <div className="space-y-1.5">
                  <Label htmlFor="ifscCode">IFSC</Label>
                  <Input
                    id="ifscCode"
                    className="uppercase"
                    value={form.ifscCode}
                    onChange={(e) => update("ifscCode", e.target.value.toUpperCase())}
                  />
                </div>
                <div className="space-y-1.5">
                  <Label htmlFor="accountHolderName">Account holder</Label>
                  <Input
                    id="accountHolderName"
                    value={form.accountHolderName}
                    onChange={(e) => update("accountHolderName", e.target.value)}
                  />
                </div>
                <div className="space-y-1.5">
                  <Label htmlFor="accountNumber">Account number</Label>
                  <Input
                    id="accountNumber"
                    value={form.accountNumber}
                    onChange={(e) => update("accountNumber", e.target.value)}
                  />
                </div>
                <div className="col-span-2 space-y-1.5">
                  <Label htmlFor="bankAddress">Bank address</Label>
                  <Textarea
                    id="bankAddress"
                    rows={2}
                    value={form.bankAddress}
                    onChange={(e) => update("bankAddress", e.target.value)}
                  />
                </div>
              </div>
            </TabsContent>

            <TabsContent value="contact" className="space-y-4 pt-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <Label htmlFor="contactName">Contact name</Label>
                  <Input
                    id="contactName"
                    value={form.contactName}
                    onChange={(e) => update("contactName", e.target.value)}
                  />
                </div>
                <div className="space-y-1.5">
                  <Label htmlFor="contactDesignation">Designation</Label>
                  <Input
                    id="contactDesignation"
                    value={form.contactDesignation}
                    onChange={(e) => update("contactDesignation", e.target.value)}
                  />
                </div>
                <div className="space-y-1.5">
                  <Label htmlFor="contactPhone">Phone</Label>
                  <Input
                    id="contactPhone"
                    value={form.contactPhone}
                    onChange={(e) => update("contactPhone", e.target.value)}
                  />
                </div>
                <div className="space-y-1.5">
                  <Label htmlFor="contactEmail">Email</Label>
                  <Input
                    id="contactEmail"
                    type="email"
                    value={form.contactEmail}
                    onChange={(e) => update("contactEmail", e.target.value)}
                  />
                  {errors.contactEmail && <p className="text-xs text-destructive">{errors.contactEmail}</p>}
                </div>
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="productDraft">Products / services supplied</Label>
                <div className="flex gap-2">
                  <Input
                    id="productDraft"
                    placeholder="e.g. HP EliteBook 840 G7 — press Enter to add"
                    value={productDraft}
                    onChange={(e) => setProductDraft(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === "Enter" || e.key === ",") {
                        e.preventDefault();
                        addProduct();
                      }
                    }}
                  />
                  <Button type="button" variant="secondary" onClick={addProduct}>
                    Add
                  </Button>
                </div>
                {form.products.length > 0 && (
                  <div className="flex flex-wrap gap-1.5 pt-1">
                    {form.products.map((p) => (
                      <span
                        key={p}
                        className="inline-flex items-center gap-1 rounded-full bg-accent px-2.5 py-1 text-xs font-medium text-accent-foreground"
                      >
                        {p}
                        <button
                          type="button"
                          onClick={() => removeProduct(p)}
                          className="rounded-full hover:bg-accent-foreground/10"
                          aria-label={`Remove ${p}`}
                        >
                          <X className="h-3 w-3" />
                        </button>
                      </span>
                    ))}
                  </div>
                )}
              </div>
            </TabsContent>
          </Tabs>

          <DialogFooter className="mt-6">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={submitting}>
              {submitting ? "Saving…" : supplier ? "Save changes" : "Add supplier"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

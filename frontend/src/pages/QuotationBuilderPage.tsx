import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { ArrowLeft, Sparkles } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { SearchSelect, type SearchSelectOption } from "@/components/common/SearchSelect";
import { QuotationStatusBadge } from "@/components/quotations/QuotationBadges";
import {
  QuotationLineItemsEditor,
  emptyLineItem,
  type LineItemForm,
} from "@/components/quotations/QuotationLineItemsEditor";
import { quotationsApi } from "@/api/quotations";
import { customersApi } from "@/api/customers";
import { suppliersApi } from "@/api/suppliers";
import { extractErrorMessage } from "@/api/client";
import {
  QUOTATION_LOCKED_STATUSES,
  QUOTATION_STATUSES,
  QUOTATION_STATUS_LABELS,
  type QuotationRequest,
  type QuotationStatus,
} from "@/types/quotation";

export function QuotationBuilderPage() {
  const { quotationId } = useParams<{ quotationId: string }>();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const isEdit = !!quotationId;

  const [customerId, setCustomerId] = useState<string | null>(searchParams.get("customerId"));
  const [customerContactId, setCustomerContactId] = useState<string | null>(null);
  const [quotationDate, setQuotationDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [validUntil, setValidUntil] = useState("");
  const [currency, setCurrency] = useState("INR");
  const [taxPercent, setTaxPercent] = useState("");
  const [termsAndConditions, setTermsAndConditions] = useState("");
  const [remarks, setRemarks] = useState("");
  const [lineItems, setLineItems] = useState<LineItemForm[]>([emptyLineItem()]);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);

  const customersQuery = useQuery({
    queryKey: ["customers", "for-quotation-picker"],
    queryFn: () => customersApi.list({ size: 500 }),
  });
  const suppliersQuery = useQuery({
    queryKey: ["suppliers", "for-quotation-picker"],
    queryFn: () => suppliersApi.list({ size: 500 }),
  });
  const quotationQuery = useQuery({
    queryKey: ["quotation", quotationId],
    queryFn: () => quotationsApi.getById(quotationId!),
    enabled: isEdit,
  });

  useEffect(() => {
    const q = quotationQuery.data;
    if (!q) return;
    setCustomerId(q.customerId);
    setCustomerContactId(q.customerContactId);
    setQuotationDate(q.quotationDate);
    setValidUntil(q.validUntil ?? "");
    setCurrency(q.currency);
    setTaxPercent(q.taxPercent != null ? String(q.taxPercent) : "");
    setTermsAndConditions(q.termsAndConditions ?? "");
    setRemarks(q.remarks ?? "");
    setLineItems(
      q.lineItems.map((li) => ({
        clientId: li.id,
        id: li.id,
        productName: li.productName,
        description: li.description ?? "",
        unit: li.unit,
        quantity: String(li.quantity),
        supplierId: li.supplierId,
        supplierPrice: li.supplierPrice != null ? String(li.supplierPrice) : "",
        sellingPrice: String(li.sellingPrice),
      })),
    );
  }, [quotationQuery.data]);

  const customers = customersQuery.data?.content ?? [];
  const suppliers = suppliersQuery.data?.content ?? [];

  const customerOptions: SearchSelectOption[] = customers.map((c) => ({
    value: c.id,
    label: c.companyName,
    sublabel: c.billingAddress.city ?? undefined,
  }));
  const supplierOptions: SearchSelectOption[] = suppliers.map((s) => ({
    value: s.id,
    label: s.companyName,
    sublabel: s.supplierCode,
  }));

  const selectedCustomer = customers.find((c) => c.id === customerId) ?? null;
  const contactOptions: SearchSelectOption[] = (selectedCustomer?.contacts ?? [])
    .filter((c) => !!c.id)
    .map((c) => ({ value: c.id as string, label: c.name, sublabel: c.contactType }));

  // Clear a selected contact that no longer belongs to the currently selected customer.
  // Gated on selectedCustomer being loaded so this doesn't race the customers query on
  // first mount and wipe out a contact restored from an existing quotation.
  useEffect(() => {
    if (!selectedCustomer) return;
    if (customerContactId && !contactOptions.some((o) => o.value === customerContactId)) {
      setCustomerContactId(null);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [customerId, selectedCustomer]);

  const totals = useMemo(() => {
    let subtotal = 0;
    let margin = 0;
    for (const item of lineItems) {
      const qty = Number(item.quantity) || 0;
      const selling = Number(item.sellingPrice) || 0;
      const lineTotal = qty * selling;
      subtotal += lineTotal;
      if (item.supplierPrice.trim() !== "") {
        margin += (selling - Number(item.supplierPrice)) * qty;
      }
    }
    const tax = taxPercent.trim() !== "" ? (subtotal * Number(taxPercent)) / 100 : 0;
    return { subtotal, tax, grandTotal: subtotal + tax, margin };
  }, [lineItems, taxPercent]);

  const locked = isEdit && quotationQuery.data ? QUOTATION_LOCKED_STATUSES.includes(quotationQuery.data.status) : false;

  const statusMutation = useMutation({
    mutationFn: (status: QuotationStatus) => quotationsApi.changeStatus(quotationId!, status),
    onSuccess: (updated) => {
      queryClient.setQueryData(["quotation", quotationId], updated);
      queryClient.invalidateQueries({ queryKey: ["quotations"] });
      toast.success(`Marked as ${QUOTATION_STATUS_LABELS[updated.status]}`);
    },
    onError: (error) => toast.error(extractErrorMessage(error, "Couldn't change status")),
  });

  function validate(): boolean {
    const nextErrors: Record<string, string> = {};
    if (!customerId) nextErrors.customerId = "Pick a customer";
    lineItems.forEach((item, idx) => {
      if (!item.productName.trim()) nextErrors[`lineItems.${idx}.productName`] = "Required";
      if (item.sellingPrice.trim() === "" || Number(item.sellingPrice) < 0) {
        nextErrors[`lineItems.${idx}.sellingPrice`] = "Required";
      }
    });
    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  }

  async function handleSave() {
    if (!validate()) {
      toast.error("Fix the highlighted fields before saving");
      return;
    }

    const payload: QuotationRequest = {
      customerId: customerId!,
      customerContactId: customerContactId || null,
      quotationDate: quotationDate || null,
      validUntil: validUntil || null,
      currency: currency.trim() || "INR",
      taxPercent: taxPercent.trim() !== "" ? Number(taxPercent) : null,
      termsAndConditions: termsAndConditions.trim() || null,
      remarks: remarks.trim() || null,
      lineItems: lineItems.map((item) => ({
        id: item.id,
        productName: item.productName.trim(),
        description: item.description.trim() || null,
        unit: item.unit.trim() || "Nos",
        quantity: Number(item.quantity) || 0,
        supplierId: item.supplierId,
        supplierPrice: item.supplierPrice.trim() !== "" ? Number(item.supplierPrice) : null,
        sellingPrice: Number(item.sellingPrice) || 0,
      })),
    };

    setSaving(true);
    try {
      const saved = isEdit ? await quotationsApi.update(quotationId!, payload) : await quotationsApi.create(payload);
      toast.success(isEdit ? "Quotation updated" : `Quotation ${saved.quotationNumber} created`);
      queryClient.invalidateQueries({ queryKey: ["quotations"] });
      queryClient.invalidateQueries({ queryKey: ["quotation", saved.id] });
      navigate(`/quotations/${saved.id}/edit`, { replace: true });
    } catch (error) {
      toast.error(extractErrorMessage(error, "Couldn't save this quotation"));
    } finally {
      setSaving(false);
    }
  }

  if (isEdit && quotationQuery.isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  return (
    <div>
      <Button
        variant="ghost"
        size="sm"
        className="mb-3 -ml-2 text-muted-foreground"
        onClick={() => navigate("/quotations")}
      >
        <ArrowLeft className="h-4 w-4" />
        Back to quotations
      </Button>

      <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-semibold tracking-tight">
            {isEdit ? quotationQuery.data?.quotationNumber ?? "Quotation" : "New quotation"}
          </h1>
          {isEdit && quotationQuery.data && (
            <p className="mt-0.5 text-sm text-muted-foreground">
              Prepared by {quotationQuery.data.preparedByName ?? "—"}
            </p>
          )}
        </div>

        {isEdit && quotationQuery.data && (
          <Select value={quotationQuery.data.status} onValueChange={(v) => statusMutation.mutate(v as QuotationStatus)}>
            <SelectTrigger className="w-44">
              <SelectValue>
                <QuotationStatusBadge status={quotationQuery.data.status} />
              </SelectValue>
            </SelectTrigger>
            <SelectContent>
              {QUOTATION_STATUSES.map((s) => (
                <SelectItem key={s} value={s}>
                  {QUOTATION_STATUS_LABELS[s]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        )}
      </div>

      {locked && (
        <Card className="mb-5 border-amber-200 bg-amber-50">
          <CardContent className="flex items-start gap-2 p-4 text-sm text-amber-800">
            <Sparkles className="mt-0.5 h-4 w-4 shrink-0" />
            <p>
              This quotation is {quotationQuery.data?.status.toLowerCase()} and its contents are locked. Change the
              status above to reopen it for editing.
            </p>
          </CardContent>
        </Card>
      )}

      <Card className="mb-4">
        <CardContent className="grid grid-cols-1 gap-4 p-5 sm:grid-cols-2 lg:grid-cols-4">
          <div className="space-y-1.5 sm:col-span-2">
            <Label>Customer</Label>
            <SearchSelect
              options={customerOptions}
              value={customerId}
              onChange={setCustomerId}
              placeholder="Search customers…"
              disabled={locked || isEdit}
            />
            {errors.customerId && <p className="text-xs text-destructive">{errors.customerId}</p>}
          </div>
          <div className="space-y-1.5 sm:col-span-2">
            <Label>Attention (contact)</Label>
            <SearchSelect
              options={contactOptions}
              value={customerContactId}
              onChange={setCustomerContactId}
              placeholder={selectedCustomer ? "Optional" : "Pick a customer first"}
              disabled={locked || !selectedCustomer}
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="quotationDate">Quotation date</Label>
            <Input
              id="quotationDate"
              type="date"
              value={quotationDate}
              disabled={locked}
              onChange={(e) => setQuotationDate(e.target.value)}
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="validUntil">Valid until</Label>
            <Input
              id="validUntil"
              type="date"
              value={validUntil}
              disabled={locked}
              onChange={(e) => setValidUntil(e.target.value)}
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="currency">Currency</Label>
            <Input id="currency" value={currency} disabled={locked} onChange={(e) => setCurrency(e.target.value)} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="taxPercent">Tax %</Label>
            <Input
              id="taxPercent"
              type="number"
              min="0"
              step="0.01"
              placeholder="e.g. 18"
              value={taxPercent}
              disabled={locked}
              onChange={(e) => setTaxPercent(e.target.value)}
            />
          </div>
        </CardContent>
      </Card>

      <Card className="mb-4">
        <CardContent className="p-5">
          <p className="mb-3 text-sm font-medium">Line items</p>
          {locked ? (
            <div className="overflow-x-auto rounded-md border border-border">
              <table className="w-full text-sm">
                <thead className="bg-muted/50 text-xs text-muted-foreground">
                  <tr>
                    <th className="px-3 py-2 text-left font-medium">Product</th>
                    <th className="px-3 py-2 text-left font-medium">Qty</th>
                    <th className="px-3 py-2 text-left font-medium">Supplier</th>
                    <th className="px-3 py-2 text-right font-medium">Selling price</th>
                    <th className="px-3 py-2 text-right font-medium">Line total</th>
                  </tr>
                </thead>
                <tbody>
                  {quotationQuery.data?.lineItems.map((li) => (
                    <tr key={li.id} className="border-t border-border">
                      <td className="px-3 py-2">{li.productName}</td>
                      <td className="px-3 py-2">
                        {li.quantity} {li.unit}
                      </td>
                      <td className="px-3 py-2">{li.supplierName ?? "—"}</td>
                      <td className="px-3 py-2 text-right">{li.sellingPrice.toFixed(2)}</td>
                      <td className="px-3 py-2 text-right font-medium">{li.lineTotal.toFixed(2)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <QuotationLineItemsEditor
              items={lineItems}
              onChange={setLineItems}
              supplierOptions={supplierOptions}
              currency={currency || "INR"}
              errors={errors}
            />
          )}
        </CardContent>
      </Card>

      <div className="mb-4 grid grid-cols-1 gap-4 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardContent className="space-y-4 p-5">
            <div className="space-y-1.5">
              <Label htmlFor="terms">Terms and conditions</Label>
              <Textarea
                id="terms"
                rows={4}
                value={termsAndConditions}
                disabled={locked}
                onChange={(e) => setTermsAndConditions(e.target.value)}
                placeholder="Payment terms, delivery, warranty…"
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="remarks">Remarks</Label>
              <Textarea
                id="remarks"
                rows={2}
                value={remarks}
                disabled={locked}
                onChange={(e) => setRemarks(e.target.value)}
              />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="space-y-2 p-5 text-sm">
            <p className="mb-1 font-medium">Summary</p>
            <div className="flex justify-between text-muted-foreground">
              <span>Subtotal</span>
              <span className="tabular-nums text-foreground">
                {currency} {totals.subtotal.toFixed(2)}
              </span>
            </div>
            <div className="flex justify-between text-muted-foreground">
              <span>Tax{taxPercent ? ` (${taxPercent}%)` : ""}</span>
              <span className="tabular-nums text-foreground">
                {currency} {totals.tax.toFixed(2)}
              </span>
            </div>
            <div className="flex justify-between border-t border-border pt-2 text-base font-semibold">
              <span>Grand total</span>
              <span className="tabular-nums">
                {currency} {totals.grandTotal.toFixed(2)}
              </span>
            </div>
            <div className="flex justify-between pt-1 text-xs text-muted-foreground">
              <span>Estimated margin</span>
              <span className="tabular-nums">
                {currency} {totals.margin.toFixed(2)}
              </span>
            </div>
          </CardContent>
        </Card>
      </div>

      {!locked && (
        <div className="flex justify-end gap-2">
          <Button variant="outline" onClick={() => navigate("/quotations")}>
            Cancel
          </Button>
          <Button onClick={handleSave} disabled={saving}>
            {saving ? "Saving…" : isEdit ? "Save changes" : "Create quotation"}
          </Button>
        </div>
      )}
    </div>
  );
}

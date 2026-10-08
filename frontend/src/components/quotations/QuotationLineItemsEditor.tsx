import { AlertTriangle, Plus, Trash2, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { SearchSelect, type SearchSelectOption } from "@/components/common/SearchSelect";
import { cn } from "@/lib/utils";

export interface LineItemForm {
  clientId: string;
  id?: string;
  productName: string;
  description: string;
  unit: string;
  quantity: string;
  supplierId: string | null;
  supplierPrice: string;
  sellingPrice: string;
  /** Set when this line's supplier/price came from "Start from requirement" — see
   *  QuotationBuilderPage.applyPrefill. Locks supplier + supplierPrice to that chosen
   *  reply (the server ignores anything typed there for a line with enquiryReplyId set
   *  anyway — see QuotationService.applyLineItemPricing — so editing them here would
   *  just be misleading). */
  enquiryReplyId: string | null;
  supplierName: string | null;
  supplierReference: string | null;
  enquiryCode: string | null;
  replyValidUntil: string | null;
}

export function emptyLineItem(): LineItemForm {
  return {
    clientId: crypto.randomUUID(),
    productName: "",
    description: "",
    unit: "Nos",
    quantity: "1",
    supplierId: null,
    supplierPrice: "",
    sellingPrice: "",
    enquiryReplyId: null,
    supplierName: null,
    supplierReference: null,
    enquiryCode: null,
    replyValidUntil: null,
  };
}

function lineTotal(item: LineItemForm): number {
  const qty = Number(item.quantity) || 0;
  const price = Number(item.sellingPrice) || 0;
  return qty * price;
}

function marginPercent(item: LineItemForm): number | null {
  const qty = Number(item.quantity) || 0;
  const selling = Number(item.sellingPrice) || 0;
  const supplier = item.supplierPrice.trim() === "" ? null : Number(item.supplierPrice);
  if (supplier === null || qty === 0 || selling === 0) return null;
  const total = selling * qty;
  const margin = (selling - supplier) * qty;
  return (margin / total) * 100;
}

function replyExpired(item: LineItemForm, today: string): boolean {
  return !!item.replyValidUntil && item.replyValidUntil < today;
}

interface QuotationLineItemsEditorProps {
  items: LineItemForm[];
  onChange: (items: LineItemForm[]) => void;
  supplierOptions: SearchSelectOption[];
  currency: string;
  errors?: Record<string, string>;
}

export function QuotationLineItemsEditor({
  items,
  onChange,
  supplierOptions,
  currency,
  errors = {},
}: QuotationLineItemsEditorProps) {
  const today = new Date().toISOString().slice(0, 10);

  function update(clientId: string, patch: Partial<LineItemForm>) {
    onChange(items.map((i) => (i.clientId === clientId ? { ...i, ...patch } : i)));
  }

  function remove(clientId: string) {
    onChange(items.filter((i) => i.clientId !== clientId));
  }

  function add() {
    onChange([...items, emptyLineItem()]);
  }

  function clearPriceSource(clientId: string) {
    update(clientId, {
      enquiryReplyId: null,
      supplierName: null,
      supplierReference: null,
      enquiryCode: null,
      replyValidUntil: null,
    });
  }

  return (
    <div className="space-y-3">
      <div className="overflow-x-auto rounded-md border border-border">
        <table className="w-full min-w-[920px] text-sm">
          <thead className="bg-muted/50 text-xs text-muted-foreground">
            <tr>
              <th className="w-[22%] px-2 py-2 text-left font-medium">Product / service</th>
              <th className="w-[10%] px-2 py-2 text-left font-medium">Qty</th>
              <th className="w-[8%] px-2 py-2 text-left font-medium">Unit</th>
              <th className="w-[18%] px-2 py-2 text-left font-medium">Supplier</th>
              <th className="w-[12%] px-2 py-2 text-right font-medium">Supplier price</th>
              <th className="w-[12%] px-2 py-2 text-right font-medium">Selling price</th>
              <th className="w-[12%] px-2 py-2 text-right font-medium">Line total / margin</th>
              <th className="w-8 px-2 py-2" />
            </tr>
          </thead>
          <tbody>
            {items.map((item, idx) => {
              const margin = marginPercent(item);
              const negativeMargin = margin !== null && margin < 0;
              const underSupplierPrice =
                item.supplierPrice.trim() !== "" && Number(item.sellingPrice) < Number(item.supplierPrice);
              const expired = replyExpired(item, today);
              const fromReply = !!item.enquiryReplyId;

              return (
                <tr key={item.clientId} className="border-t border-border align-top">
                  <td className="px-2 py-2">
                    <Input
                      value={item.productName}
                      placeholder="e.g. HP EliteBook 840 G7"
                      onChange={(e) => update(item.clientId, { productName: e.target.value })}
                    />
                    {errors[`lineItems.${idx}.productName`] && (
                      <p className="mt-1 text-xs text-destructive">{errors[`lineItems.${idx}.productName`]}</p>
                    )}
                    <Input
                      value={item.description}
                      placeholder="Description (optional)"
                      className="mt-1 h-7 text-xs"
                      onChange={(e) => update(item.clientId, { description: e.target.value })}
                    />
                  </td>
                  <td className="px-2 py-2">
                    <Input
                      type="number"
                      min="0"
                      step="0.001"
                      value={item.quantity}
                      onChange={(e) => update(item.clientId, { quantity: e.target.value })}
                    />
                  </td>
                  <td className="px-2 py-2">
                    <Input value={item.unit} onChange={(e) => update(item.clientId, { unit: e.target.value })} />
                  </td>
                  <td className="px-2 py-2">
                    {fromReply ? (
                      <div className="rounded-md border border-dashed border-border bg-muted/40 p-1.5 text-xs">
                        <div className="flex items-start justify-between gap-1">
                          <div>
                            <p className="font-medium">{item.supplierName}</p>
                            <p className="text-muted-foreground">
                              {item.enquiryCode}
                              {item.supplierReference ? ` · ref ${item.supplierReference}` : ""}
                            </p>
                          </div>
                          <button
                            type="button"
                            title="Unlock and pick a different supplier"
                            onClick={() => clearPriceSource(item.clientId)}
                            className="shrink-0 rounded p-0.5 text-muted-foreground hover:bg-accent"
                          >
                            <X className="h-3 w-3" />
                          </button>
                        </div>
                        {expired && (
                          <p className="mt-1 flex items-center gap-1 text-amber-700">
                            <AlertTriangle className="h-3 w-3 shrink-0" />
                            Price expired {item.replyValidUntil}
                          </p>
                        )}
                      </div>
                    ) : (
                      <SearchSelect
                        options={supplierOptions}
                        value={item.supplierId}
                        onChange={(v) => update(item.clientId, { supplierId: v })}
                        placeholder="None"
                      />
                    )}
                  </td>
                  <td className="px-2 py-2">
                    <Input
                      type="number"
                      min="0"
                      step="0.01"
                      className="text-right"
                      value={item.supplierPrice}
                      disabled={fromReply}
                      onChange={(e) => update(item.clientId, { supplierPrice: e.target.value })}
                    />
                  </td>
                  <td className="px-2 py-2">
                    <Input
                      type="number"
                      min="0"
                      step="0.01"
                      className={cn("text-right", (negativeMargin || underSupplierPrice) && "border-amber-400")}
                      value={item.sellingPrice}
                      onChange={(e) => update(item.clientId, { sellingPrice: e.target.value })}
                    />
                    {errors[`lineItems.${idx}.sellingPrice`] && (
                      <p className="mt-1 text-right text-xs text-destructive">
                        {errors[`lineItems.${idx}.sellingPrice`]}
                      </p>
                    )}
                  </td>
                  <td className="px-2 py-2 text-right">
                    <p className="font-medium tabular-nums">
                      {currency} {lineTotal(item).toFixed(2)}
                    </p>
                    {margin !== null && (
                      <p className={negativeMargin ? "text-xs font-medium text-amber-700" : "text-xs text-emerald-700"}>
                        {negativeMargin && <AlertTriangle className="mr-0.5 inline h-3 w-3" />}
                        {margin.toFixed(1)}% margin
                      </p>
                    )}
                  </td>
                  <td className="px-2 py-2">
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      disabled={items.length === 1}
                      onClick={() => remove(item.clientId)}
                    >
                      <Trash2 className="h-4 w-4 text-muted-foreground" />
                    </Button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      <Button type="button" variant="outline" size="sm" onClick={add}>
        <Plus className="h-4 w-4" />
        Add line item
      </Button>
    </div>
  );
}

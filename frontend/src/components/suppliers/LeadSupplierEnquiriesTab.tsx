import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Boxes, CheckCircle2, Mail, Plus, Star } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/common/EmptyState";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
  DialogTrigger,
} from "@/components/ui/dialog";
import { SupplierEnquiryReplyDialog } from "@/components/suppliers/SupplierEnquiryReplyDialog";
import { SearchSelect, type SearchSelectOption } from "@/components/common/SearchSelect";
import { supplierEnquiriesApi } from "@/api/supplierEnquiries";
import { suppliersApi } from "@/api/suppliers";
import { requirementsApi } from "@/api/requirements";
import { extractErrorMessage } from "@/api/client";
import { cn, formatDate } from "@/lib/utils";
import { SUPPLIER_REPLY_STATUS_LABELS, type SupplierEnquiryReplyResponse } from "@/types/supplierEnquiry";

const REPLY_STATUS_CLASS: Record<SupplierEnquiryReplyResponse["status"], string> = {
  PENDING: "bg-muted text-muted-foreground",
  REPLIED: "bg-sky-100 text-sky-700",
  DECLINED: "bg-red-100 text-red-700",
};

function NewEnquiryDialog({ leadId, open, onOpenChange }: { leadId: string; open: boolean; onOpenChange: (o: boolean) => void }) {
  const queryClient = useQueryClient();
  const [itemDescription, setItemDescription] = useState("");
  const [quantity, setQuantity] = useState("");
  const [notes, setNotes] = useState("");
  const [requirementId, setRequirementId] = useState<string | null>(null);
  const [selectedSupplierIds, setSelectedSupplierIds] = useState<string[]>([]);

  // Suppliers are org-wide master data (see Supplier.java's class comment) — a
  // plain list of the first 100, active ones, is plenty for a picker checklist.
  const suppliersQuery = useQuery({
    queryKey: ["suppliers", "picker"],
    queryFn: () => suppliersApi.list({ size: 100 }),
    enabled: open,
  });
  // Optional - lets this enquiry's chosen reply later be found by
  // QuotationService.prefill instead of just living as a free-text item.
  const requirementsQuery = useQuery({
    queryKey: ["requirements", "by-lead", leadId],
    queryFn: () => requirementsApi.byLead(leadId, 0, 100),
    enabled: open,
  });
  const requirementOptions: SearchSelectOption[] = (requirementsQuery.data?.content ?? []).map((r) => ({
    value: r.id,
    label: r.productName,
    sublabel: r.quantity != null ? `${r.quantity} ${r.unit ?? ""}`.trim() : r.status,
  }));

  const create = useMutation({
    mutationFn: () =>
      supplierEnquiriesApi.create({
        leadId,
        requirementId,
        itemDescription: itemDescription.trim(),
        quantity: quantity.trim() || null,
        notes: notes.trim() || null,
        supplierIds: selectedSupplierIds,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["supplier-enquiries", "lead", leadId] });
      toast.success("Enquiry sent to " + selectedSupplierIds.length + " supplier(s)");
      setItemDescription("");
      setQuantity("");
      setNotes("");
      setRequirementId(null);
      setSelectedSupplierIds([]);
      onOpenChange(false);
    },
    onError: (error) => toast.error(extractErrorMessage(error, "Couldn't send this enquiry")),
  });

  function applyRequirement(id: string | null) {
    setRequirementId(id);
    const requirement = requirementsQuery.data?.content.find((r) => r.id === id);
    if (requirement && !itemDescription.trim()) {
      setItemDescription(requirement.productName);
      if (requirement.quantity != null) {
        setQuantity(`${requirement.quantity} ${requirement.unit ?? ""}`.trim());
      }
    }
  }

  function toggleSupplier(id: string) {
    setSelectedSupplierIds((prev) => (prev.includes(id) ? prev.filter((s) => s !== id) : [...prev, id]));
  }

  const canSubmit = itemDescription.trim().length > 0 && selectedSupplierIds.length > 0;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogTrigger asChild>
        <Button size="sm">
          <Plus className="h-4 w-4" />
          New enquiry
        </Button>
      </DialogTrigger>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>Send a supplier enquiry</DialogTitle>
          <DialogDescription>
            Describe what this lead needs and pick which suppliers to ask for a price.
          </DialogDescription>
        </DialogHeader>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            if (canSubmit) create.mutate();
          }}
          className="space-y-3"
        >
          <div className="space-y-1.5">
            <Label>Link to a requirement (optional)</Label>
            <SearchSelect
              options={requirementOptions}
              value={requirementId}
              onChange={applyRequirement}
              placeholder={requirementOptions.length === 0 ? "This lead has no requirements yet" : "None"}
              disabled={requirementOptions.length === 0}
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="enq-item">Item / requirement</Label>
            <Input
              id="enq-item"
              placeholder="e.g. 500 units of 6mm PVC sheet"
              value={itemDescription}
              onChange={(e) => setItemDescription(e.target.value)}
              autoFocus
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="enq-qty">Quantity</Label>
            <Input id="enq-qty" placeholder="e.g. 500 units" value={quantity} onChange={(e) => setQuantity(e.target.value)} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="enq-notes">Notes</Label>
            <Textarea id="enq-notes" rows={2} value={notes} onChange={(e) => setNotes(e.target.value)} />
          </div>
          <div className="space-y-1.5">
            <Label>Send to</Label>
            {suppliersQuery.isLoading ? (
              <Skeleton className="h-24 w-full" />
            ) : (
              <div className="max-h-48 space-y-1 overflow-y-auto rounded-md border border-border p-2 thin-scrollbar">
                {suppliersQuery.data?.content.length === 0 && (
                  <p className="p-2 text-sm text-muted-foreground">No suppliers yet — add one first.</p>
                )}
                {suppliersQuery.data?.content.map((s) => (
                  <label
                    key={s.id}
                    className="flex cursor-pointer items-center gap-2 rounded px-2 py-1.5 text-sm hover:bg-accent"
                  >
                    <input
                      type="checkbox"
                      className="h-4 w-4 accent-primary"
                      checked={selectedSupplierIds.includes(s.id)}
                      onChange={() => toggleSupplier(s.id)}
                    />
                    <span className="flex-1 truncate">{s.companyName}</span>
                    {!s.contactEmail && <span className="text-xs text-muted-foreground">no email on file</span>}
                  </label>
                ))}
              </div>
            )}
            <p className="text-xs text-muted-foreground">{selectedSupplierIds.length} selected</p>
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={!canSubmit || create.isPending}>
              {create.isPending ? "Sending…" : "Send enquiry"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

export function LeadSupplierEnquiriesTab({ leadId }: { leadId: string }) {
  const queryClient = useQueryClient();
  const [newOpen, setNewOpen] = useState(false);
  const [activeReply, setActiveReply] = useState<{ enquiryId: string; reply: SupplierEnquiryReplyResponse } | null>(null);

  const enquiriesQuery = useQuery({
    queryKey: ["supplier-enquiries", "lead", leadId],
    queryFn: () => supplierEnquiriesApi.byLead(leadId),
  });

  const chooseReply = useMutation({
    mutationFn: ({ enquiryId, replyId }: { enquiryId: string; replyId: string }) =>
      supplierEnquiriesApi.chooseReply(enquiryId, replyId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["supplier-enquiries", "lead", leadId] });
      toast.success("Set as the price reference for this enquiry");
    },
    onError: (error) => toast.error(extractErrorMessage(error, "Couldn't choose this reply")),
  });

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <p className="text-sm text-muted-foreground">
          Source items this lead needs from your suppliers — send one enquiry, compare what comes back, and pick
          the price to carry into the quotation.
        </p>
        <NewEnquiryDialog leadId={leadId} open={newOpen} onOpenChange={setNewOpen} />
      </div>

      {enquiriesQuery.isLoading ? (
        <div className="space-y-2">
          <Skeleton className="h-28 w-full" />
          <Skeleton className="h-28 w-full" />
        </div>
      ) : enquiriesQuery.data && enquiriesQuery.data.content.length > 0 ? (
        <div className="space-y-3">
          {enquiriesQuery.data.content.map((enquiry) => (
            <div key={enquiry.id} className="rounded-lg border border-border">
              <div className="flex items-start justify-between gap-3 border-b border-border p-3">
                <div className="min-w-0">
                  <p className="flex items-center gap-2 text-sm font-medium">
                    {enquiry.itemDescription}
                    <span className="rounded bg-muted px-1.5 py-0.5 text-xs font-normal text-muted-foreground">
                      {enquiry.enquiryCode}
                    </span>
                  </p>
                  <p className="mt-0.5 text-xs text-muted-foreground">
                    {enquiry.quantity && `Qty: ${enquiry.quantity} · `}
                    Sent {formatDate(enquiry.createdAt, true)}
                    {enquiry.createdByName && ` by ${enquiry.createdByName}`}
                  </p>
                </div>
                <span
                  className={cn(
                    "shrink-0 rounded-full px-2 py-0.5 text-xs font-medium",
                    enquiry.status === "CLOSED" ? "bg-emerald-100 text-emerald-700" : "bg-amber-100 text-amber-700",
                  )}
                >
                  {enquiry.status === "CLOSED" ? "Price chosen" : "Awaiting replies"}
                </span>
              </div>

              <div className="divide-y divide-border">
                {enquiry.replies.map((reply) => (
                  <div key={reply.id} className="flex flex-wrap items-center justify-between gap-2 p-3">
                    <div className="flex min-w-0 items-center gap-2">
                      {reply.chosen && <Star className="h-4 w-4 shrink-0 fill-amber-400 text-amber-400" />}
                      <div className="min-w-0">
                        <p className="truncate text-sm font-medium">{reply.supplierName}</p>
                        <p className="flex items-center gap-1 text-xs text-muted-foreground">
                          {reply.emailSentAt ? (
                            <>
                              <Mail className="h-3 w-3" /> Enquiry sent
                            </>
                          ) : (
                            "No email on file — contact manually"
                          )}
                        </p>
                      </div>
                    </div>

                    <div className="flex items-center gap-2">
                      {reply.price != null && (
                        <span className="text-sm font-semibold tabular-nums">
                          {reply.currency} {reply.price.toLocaleString("en-IN")}
                        </span>
                      )}
                      <span className={cn("rounded-full px-2 py-0.5 text-xs font-medium", REPLY_STATUS_CLASS[reply.status])}>
                        {SUPPLIER_REPLY_STATUS_LABELS[reply.status]}
                      </span>
                      {reply.status === "PENDING" && (
                        <Button variant="outline" size="sm" onClick={() => setActiveReply({ enquiryId: enquiry.id, reply })}>
                          Record reply
                        </Button>
                      )}
                      {reply.status === "REPLIED" && !reply.chosen && (
                        <Button
                          variant="outline"
                          size="sm"
                          disabled={chooseReply.isPending}
                          onClick={() => chooseReply.mutate({ enquiryId: enquiry.id, replyId: reply.id })}
                        >
                          <CheckCircle2 className="h-3.5 w-3.5" />
                          Choose this price
                        </Button>
                      )}
                      {reply.status === "REPLIED" && (
                        <Button variant="ghost" size="sm" onClick={() => setActiveReply({ enquiryId: enquiry.id, reply })}>
                          Edit
                        </Button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      ) : (
        <EmptyState
          icon={Boxes}
          title="No supplier enquiries yet"
          description="When this lead needs something you don't have in stock, send an enquiry to your suppliers to get pricing."
          action={
            <Button size="sm" className="mt-1" onClick={() => setNewOpen(true)}>
              <Plus className="h-4 w-4" />
              New enquiry
            </Button>
          }
        />
      )}

      {activeReply && (
        <SupplierEnquiryReplyDialog
          enquiryId={activeReply.enquiryId}
          reply={activeReply.reply}
          open={!!activeReply}
          onOpenChange={(open) => !open && setActiveReply(null)}
        />
      )}
    </div>
  );
}

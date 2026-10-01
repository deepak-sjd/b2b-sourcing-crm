import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
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
import { Textarea } from "@/components/ui/textarea";
import { supplierEnquiriesApi } from "@/api/supplierEnquiries";
import { extractErrorMessage } from "@/api/client";
import type { SupplierEnquiryReplyResponse } from "@/types/supplierEnquiry";

interface SupplierEnquiryReplyDialogProps {
  enquiryId: string;
  reply: SupplierEnquiryReplyResponse;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

/** PRD 12.2's "Supplier Reply" fields — price, currency, validity, delivery,
 *  the supplier's own reference number, terms, remarks — logged against one
 *  reply row. A Decline button covers "supplier can't quote this". */
export function SupplierEnquiryReplyDialog({ enquiryId, reply, open, onOpenChange }: SupplierEnquiryReplyDialogProps) {
  const queryClient = useQueryClient();
  const [price, setPrice] = useState(reply.price != null ? String(reply.price) : "");
  const [currency, setCurrency] = useState(reply.currency || "INR");
  const [validUntil, setValidUntil] = useState(reply.validUntil ?? "");
  const [deliveryInfo, setDeliveryInfo] = useState(reply.deliveryInfo ?? "");
  const [supplierReference, setSupplierReference] = useState(reply.supplierReference ?? "");
  const [terms, setTerms] = useState(reply.terms ?? "");
  const [remarks, setRemarks] = useState(reply.remarks ?? "");

  const save = useMutation({
    mutationFn: (status: "REPLIED" | "DECLINED") =>
      supplierEnquiriesApi.recordReply(enquiryId, reply.id, {
        status,
        price: status === "REPLIED" && price ? Number(price) : null,
        currency,
        validUntil: validUntil || null,
        deliveryInfo: deliveryInfo || null,
        supplierReference: supplierReference || null,
        terms: terms || null,
        remarks: remarks || null,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["supplier-enquiries"] });
      toast.success("Reply recorded");
      onOpenChange(false);
    },
    onError: (error) => toast.error(extractErrorMessage(error, "Couldn't save this reply")),
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{reply.supplierName}'s reply</DialogTitle>
          <DialogDescription>Log the price they quoted, or mark them as unable to supply this.</DialogDescription>
        </DialogHeader>

        <div className="space-y-3">
          <div className="grid grid-cols-3 gap-3">
            <div className="col-span-2 space-y-1.5">
              <Label htmlFor="reply-price">Price</Label>
              <Input id="reply-price" type="number" min="0" value={price} onChange={(e) => setPrice(e.target.value)} />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="reply-currency">Currency</Label>
              <Input id="reply-currency" value={currency} onChange={(e) => setCurrency(e.target.value)} />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <Label htmlFor="reply-valid">Valid until</Label>
              <Input id="reply-valid" type="date" value={validUntil} onChange={(e) => setValidUntil(e.target.value)} />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="reply-ref">Supplier reference #</Label>
              <Input id="reply-ref" value={supplierReference} onChange={(e) => setSupplierReference(e.target.value)} />
            </div>
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="reply-delivery">Delivery info</Label>
            <Input id="reply-delivery" value={deliveryInfo} onChange={(e) => setDeliveryInfo(e.target.value)} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="reply-terms">Terms</Label>
            <Textarea id="reply-terms" rows={2} value={terms} onChange={(e) => setTerms(e.target.value)} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="reply-remarks">Remarks</Label>
            <Textarea id="reply-remarks" rows={2} value={remarks} onChange={(e) => setRemarks(e.target.value)} />
          </div>
        </div>

        <DialogFooter className="gap-2 sm:justify-between">
          <Button
            type="button"
            variant="outline"
            className="text-destructive hover:text-destructive"
            disabled={save.isPending}
            onClick={() => save.mutate("DECLINED")}
          >
            Mark as declined
          </Button>
          <Button type="button" disabled={save.isPending || !price} onClick={() => save.mutate("REPLIED")}>
            {save.isPending ? "Saving…" : "Save reply"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { AlertTriangle, CheckCircle2, Mail, Plus, Star, X } from "lucide-react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/common/EmptyState";
import { RecordReplyDialog } from "@/components/enquiries/RecordReplyDialog";
import { SendEnquiryDialog } from "@/components/enquiries/SendEnquiryDialog";
import { supplierEnquiriesApi } from "@/api/supplierEnquiries";
import { extractErrorMessage } from "@/api/client";
import { useAuthStore } from "@/store/authStore";
import { cn, formatDate } from "@/lib/utils";
import { SUPPLIER_REPLY_STATUS_LABELS, type SupplierEnquiryReplyResponse } from "@/types/supplierEnquiry";
import type { Requirement } from "@/types/requirement";

interface RequirementSourcingDialogProps {
  requirement: Requirement;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

function lineTotal(price: number, quantity: number | null): string | null {
  if (quantity == null) return null;
  return (price * quantity).toLocaleString("en-IN", { maximumFractionDigits: 2 });
}

export function RequirementSourcingDialog({ requirement, open, onOpenChange }: RequirementSourcingDialogProps) {
  const queryClient = useQueryClient();
  const role = useAuthStore((s) => s.user?.role);
  const canWrite = role !== "VIEWER";
  const [newEnquiryOpen, setNewEnquiryOpen] = useState(false);
  const [activeReply, setActiveReply] = useState<{ enquiryId: string; reply: SupplierEnquiryReplyResponse } | null>(null);

  const query = useQuery({
    queryKey: ["supplier-enquiries", "requirement", requirement.id],
    queryFn: () => supplierEnquiriesApi.byRequirement(requirement.id),
    enabled: open,
  });

  const chosenReply = useMemo(() => {
    for (const enquiry of query.data ?? []) {
      const found = enquiry.replies.find((r) => r.chosen);
      if (found) return found;
    }
    return null;
  }, [query.data]);

  const choose = useMutation({
    mutationFn: ({ enquiryId, replyId }: { enquiryId: string; replyId: string }) =>
      supplierEnquiriesApi.choose(enquiryId, replyId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["supplier-enquiries"] });
      toast.success("Set as the price reference");
    },
    onError: (error) => toast.error(extractErrorMessage(error, "Couldn't choose this price")),
  });

  const unchoose = useMutation({
    mutationFn: ({ enquiryId, replyId }: { enquiryId: string; replyId: string }) =>
      supplierEnquiriesApi.unchoose(enquiryId, replyId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["supplier-enquiries"] });
      toast.success("Price reference cleared");
    },
    onError: (error) => toast.error(extractErrorMessage(error, "Couldn't clear the reference")),
  });

  const resend = useMutation({
    mutationFn: ({ enquiryId, replyId }: { enquiryId: string; replyId: string }) =>
      supplierEnquiriesApi.resend(enquiryId, replyId),
    onSuccess: (reply) => {
      queryClient.invalidateQueries({ queryKey: ["supplier-enquiries"] });
      toast[reply.emailSent ? "success" : "warning"](reply.emailSent ? "Enquiry resent" : "Resend failed again");
    },
    onError: (error) => toast.error(extractErrorMessage(error, "Couldn't resend")),
  });

  function lowestReplyIds(replies: SupplierEnquiryReplyResponse[]): Set<string> {
    const byCurrency = new Map<string, SupplierEnquiryReplyResponse[]>();
    for (const r of replies) {
      if (r.status !== "REPLIED" || r.price == null || r.expired) continue;
      const list = byCurrency.get(r.currency) ?? [];
      list.push(r);
      byCurrency.set(r.currency, list);
    }
    const lowest = new Set<string>();
    for (const list of byCurrency.values()) {
      if (list.length < 2) continue;
      const min = list.reduce((a, b) => ((a.price ?? Infinity) <= (b.price ?? Infinity) ? a : b));
      lowest.add(min.id);
    }
    return lowest;
  }

  return (
    <>
      <Dialog open={open} onOpenChange={onOpenChange}>
        <DialogContent className="max-w-3xl">
          <DialogHeader>
            <DialogTitle>Supplier sourcing — {requirement.productName}</DialogTitle>
            <DialogDescription>Compare prices across every enquiry raised for this requirement.</DialogDescription>
          </DialogHeader>

          {chosenReply && (
            <div className="flex items-center gap-2 rounded-md bg-emerald-50 p-2.5 text-sm text-emerald-800">
              <CheckCircle2 className="h-4 w-4 shrink-0" />
              <span>
                Price reference: <strong>{chosenReply.supplierName}</strong> at {chosenReply.currency}{" "}
                {chosenReply.price?.toLocaleString("en-IN")}
                {chosenReply.supplierReference && <> (their ref {chosenReply.supplierReference})</>}
              </span>
            </div>
          )}

          <div className="flex items-center justify-between">
            <p className="text-xs text-muted-foreground">
              {query.data?.length ?? 0} enquir{(query.data?.length ?? 0) === 1 ? "y" : "ies"} raised
            </p>
            {canWrite && (
              <Button size="sm" onClick={() => setNewEnquiryOpen(true)}>
                <Plus className="h-4 w-4" />
                New enquiry
              </Button>
            )}
          </div>

          <div className="max-h-[55vh] space-y-4 overflow-y-auto thin-scrollbar">
            {query.isLoading ? (
              <Skeleton className="h-40 w-full" />
            ) : !query.data || query.data.length === 0 ? (
              <EmptyState
                title="No enquiries yet"
                description="Send a price enquiry to one or more suppliers to start comparing."
              />
            ) : (
              query.data.map((enquiry) => {
                const lowestIds = lowestReplyIds(enquiry.replies);
                return (
                  <div key={enquiry.id} className="rounded-lg border border-border">
                    <div className="flex items-center justify-between border-b border-border px-3 py-2">
                      <p className="text-xs font-medium text-muted-foreground">
                        {enquiry.enquiryCode} · {formatDate(enquiry.createdAt, true)}
                        {enquiry.responseDueDate && <> · reply by {formatDate(enquiry.responseDueDate)}</>}
                      </p>
                      <span
                        className={cn(
                          "rounded-full px-2 py-0.5 text-[11px] font-medium",
                          enquiry.status === "CLOSED" ? "bg-emerald-100 text-emerald-700" : "bg-amber-100 text-amber-700",
                        )}
                      >
                        {enquiry.status === "CLOSED" ? "Price chosen" : "Awaiting replies"}
                      </span>
                    </div>
                    <div className="divide-y divide-border">
                      {enquiry.replies.map((reply) => {
                        const total = reply.price != null ? lineTotal(reply.price, requirement.quantity) : null;
                        return (
                          <div key={reply.id} className="space-y-1.5 p-3">
                            <div className="flex flex-wrap items-center justify-between gap-2">
                              <div className="flex min-w-0 items-center gap-2">
                                {reply.chosen && <Star className="h-4 w-4 shrink-0 fill-amber-400 text-amber-400" />}
                                <div className="min-w-0">
                                  <p className="truncate text-sm font-medium">
                                    {reply.supplierName}{" "}
                                    <span className="text-xs font-normal text-muted-foreground">({reply.supplierCode})</span>
                                  </p>
                                  <p className="truncate text-xs text-muted-foreground">{reply.supplierContactEmail ?? "No email"}</p>
                                </div>
                              </div>
                              <div className="flex items-center gap-2">
                                {reply.price != null && (
                                  <div className="text-right">
                                    <p className="text-sm font-semibold tabular-nums">
                                      {reply.currency} {reply.price.toLocaleString("en-IN")}
                                      {lowestIds.has(reply.id) && (
                                        <span className="ml-1.5 rounded-full bg-emerald-100 px-1.5 py-0.5 text-[10px] font-medium text-emerald-700">
                                          Lowest
                                        </span>
                                      )}
                                    </p>
                                    {total && <p className="text-[11px] text-muted-foreground">Total: {reply.currency} {total}</p>}
                                  </div>
                                )}
                                <span
                                  className={cn(
                                    "rounded-full px-2 py-0.5 text-[11px] font-medium",
                                    reply.status === "REPLIED" && "bg-sky-100 text-sky-700",
                                    reply.status === "PENDING" && "bg-muted text-muted-foreground",
                                    reply.status === "DECLINED" && "bg-red-100 text-red-700",
                                  )}
                                >
                                  {SUPPLIER_REPLY_STATUS_LABELS[reply.status]}
                                </span>
                              </div>
                            </div>

                            <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-muted-foreground">
                              {reply.validUntil && (
                                <span className={cn(reply.expired && "font-medium text-destructive")}>
                                  Valid until {formatDate(reply.validUntil)}
                                  {reply.expired && " (expired)"}
                                </span>
                              )}
                              {reply.deliveryInfo && <span>Delivery: {reply.deliveryInfo}</span>}
                              {reply.supplierReference && <span>Ref: {reply.supplierReference}</span>}
                            </div>

                            {reply.emailError && (
                              <div className="flex items-center gap-1.5 rounded bg-amber-50 px-2 py-1 text-xs text-amber-800">
                                <AlertTriangle className="h-3 w-3 shrink-0" />
                                {reply.emailError}
                              </div>
                            )}

                            {canWrite && (
                              <div className="flex flex-wrap gap-2 pt-1">
                                {reply.status === "PENDING" && reply.supplierContactEmail && (
                                  <Button
                                    variant="outline"
                                    size="sm"
                                    className="h-7 gap-1"
                                    disabled={resend.isPending}
                                    onClick={() => resend.mutate({ enquiryId: enquiry.id, replyId: reply.id })}
                                  >
                                    <Mail className="h-3 w-3" />
                                    Resend
                                  </Button>
                                )}
                                {reply.status !== "DECLINED" && (
                                  <Button
                                    variant="outline"
                                    size="sm"
                                    className="h-7"
                                    onClick={() => setActiveReply({ enquiryId: enquiry.id, reply })}
                                  >
                                    {reply.status === "PENDING" ? "Record reply" : "Edit reply"}
                                  </Button>
                                )}
                                {reply.status === "REPLIED" && !reply.chosen && (
                                  <Button
                                    variant="outline"
                                    size="sm"
                                    className="h-7 gap-1"
                                    disabled={
                                      choose.isPending ||
                                      reply.expired ||
                                      requirement.status === "CLOSED" ||
                                      requirement.status === "CANCELLED"
                                    }
                                    onClick={() => choose.mutate({ enquiryId: enquiry.id, replyId: reply.id })}
                                  >
                                    <CheckCircle2 className="h-3 w-3" />
                                    Use this price
                                  </Button>
                                )}
                                {reply.chosen && (
                                  <Button
                                    variant="outline"
                                    size="sm"
                                    className="h-7 gap-1 text-muted-foreground"
                                    disabled={unchoose.isPending}
                                    onClick={() => unchoose.mutate({ enquiryId: enquiry.id, replyId: reply.id })}
                                  >
                                    <X className="h-3 w-3" />
                                    Clear reference
                                  </Button>
                                )}
                              </div>
                            )}
                          </div>
                        );
                      })}
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </DialogContent>
      </Dialog>

      <SendEnquiryDialog requirement={requirement} open={newEnquiryOpen} onOpenChange={setNewEnquiryOpen} />
      {activeReply && (
        <RecordReplyDialog
          enquiryId={activeReply.enquiryId}
          reply={activeReply.reply}
          open={!!activeReply}
          onOpenChange={(open) => !open && setActiveReply(null)}
        />
      )}
    </>
  );
}

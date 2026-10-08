import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Search, Sparkles } from "lucide-react";
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
import { Skeleton } from "@/components/ui/skeleton";
import { supplierEnquiriesApi } from "@/api/supplierEnquiries";
import { suppliersApi } from "@/api/suppliers";
import { extractErrorMessage } from "@/api/client";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import { cn } from "@/lib/utils";
import type { Requirement } from "@/types/requirement";

interface SendEnquiryDialogProps {
  requirement: Requirement;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

const STOP_WORDS = new Set(["the", "and", "for", "with", "from"]);

/** A supplier "supplies this" when one of their product tags shares a
 *  significant (3+ letter) word with the requirement's product name - a
 *  rough but useful nudge, not a hard filter. */
function wordOverlap(a: string, b: string): boolean {
  const wordsA = a.toLowerCase().split(/\W+/).filter((w) => w.length >= 3 && !STOP_WORDS.has(w));
  const wordsB = new Set(b.toLowerCase().split(/\W+/).filter((w) => w.length >= 3 && !STOP_WORDS.has(w)));
  return wordsA.some((w) => wordsB.has(w));
}

export function SendEnquiryDialog({ requirement, open, onOpenChange }: SendEnquiryDialogProps) {
  const queryClient = useQueryClient();
  const [search, setSearch] = useState("");
  const debouncedSearch = useDebouncedValue(search, 250);
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [message, setMessage] = useState("");
  const [responseDueDate, setResponseDueDate] = useState("");

  const suppliersQuery = useQuery({
    queryKey: ["suppliers", "enquiry-picker", debouncedSearch],
    queryFn: () => suppliersApi.list({ q: debouncedSearch, size: 50 }),
    enabled: open,
  });

  const usableSuppliers = useMemo(
    () => (suppliersQuery.data?.content ?? []).filter((s) => s.status !== "ON_HOLD" && s.status !== "BLACKLISTED"),
    [suppliersQuery.data],
  );

  const suggestedIds = useMemo(
    () =>
      new Set(
        usableSuppliers
          .filter((s) => s.products.some((p) => wordOverlap(p, requirement.productName)))
          .map((s) => s.id),
      ),
    [usableSuppliers, requirement.productName],
  );

  const send = useMutation({
    mutationFn: () =>
      supplierEnquiriesApi.create({
        requirementId: requirement.id,
        supplierIds: selectedIds,
        message: message.trim() || null,
        responseDueDate: responseDueDate || null,
      }),
    onSuccess: (enquiry) => {
      queryClient.invalidateQueries({ queryKey: ["supplier-enquiries"] });
      const failed = enquiry.replies.filter((r) => !r.emailSent && r.supplierContactEmail);
      if (failed.length > 0) {
        toast.warning(
          `Sent, but delivery failed for ${failed.length} supplier(s). You can resend or record their reply by phone.`,
        );
      } else {
        toast.success(`Enquiry sent to ${enquiry.replies.length} supplier(s)`);
      }
      setSelectedIds([]);
      setMessage("");
      setResponseDueDate("");
      onOpenChange(false);
    },
    onError: (error) => toast.error(extractErrorMessage(error, "Couldn't send this enquiry")),
  });

  function toggle(id: string) {
    setSelectedIds((prev) => (prev.includes(id) ? prev.filter((s) => s !== id) : [...prev, id]));
  }

  function selectSuggested() {
    setSelectedIds((prev) => Array.from(new Set([...prev, ...suggestedIds])));
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>Send a supplier enquiry</DialogTitle>
          <DialogDescription>
            Sourcing for <span className="font-medium text-foreground">{requirement.productName}</span>. Suppliers
            only see the product, quantity and your message — never the customer's name or budget.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-3">
          <div className="relative">
            <Search className="pointer-events-none absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Search suppliers…"
              className="pl-8"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>

          {suggestedIds.size > 0 && (
            <Button type="button" variant="outline" size="sm" className="gap-1.5" onClick={selectSuggested}>
              <Sparkles className="h-3.5 w-3.5" />
              Select {suggestedIds.size} suggested supplier{suggestedIds.size > 1 ? "s" : ""}
            </Button>
          )}

          {suppliersQuery.isLoading ? (
            <Skeleton className="h-48 w-full" />
          ) : (
            <div className="max-h-56 space-y-1 overflow-y-auto rounded-md border border-border p-2 thin-scrollbar">
              {usableSuppliers.length === 0 && (
                <p className="p-2 text-sm text-muted-foreground">No suppliers match.</p>
              )}
              {usableSuppliers.map((s) => (
                <label
                  key={s.id}
                  className="flex cursor-pointer items-center gap-2 rounded px-2 py-1.5 text-sm hover:bg-accent"
                >
                  <input
                    type="checkbox"
                    className="h-4 w-4 shrink-0 accent-primary"
                    checked={selectedIds.includes(s.id)}
                    onChange={() => toggle(s.id)}
                  />
                  <span className="min-w-0 flex-1 truncate">{s.companyName}</span>
                  {suggestedIds.has(s.id) && (
                    <span className="shrink-0 rounded-full bg-emerald-100 px-1.5 py-0.5 text-[10px] font-medium text-emerald-700">
                      Supplies this
                    </span>
                  )}
                  {s.status === "PENDING_VERIFICATION" && (
                    <span className="shrink-0 rounded-full bg-amber-100 px-1.5 py-0.5 text-[10px] font-medium text-amber-700">
                      Unverified
                    </span>
                  )}
                  {!s.contactEmail && (
                    <span className="shrink-0 text-[10px] text-muted-foreground">no email on file</span>
                  )}
                </label>
              ))}
            </div>
          )}
          <p className={cn("text-xs text-muted-foreground", selectedIds.length === 0 && "text-amber-600")}>
            {selectedIds.length} selected
          </p>

          <div className="grid grid-cols-2 gap-3">
            <div className="col-span-2 space-y-1.5">
              <Label htmlFor="enq-message">Message (optional)</Label>
              <Textarea id="enq-message" rows={2} value={message} onChange={(e) => setMessage(e.target.value)} />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="enq-due">Reply by (optional)</Label>
              <Input
                id="enq-due"
                type="date"
                value={responseDueDate}
                onChange={(e) => setResponseDueDate(e.target.value)}
              />
            </div>
          </div>
        </div>

        <DialogFooter>
          <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button type="button" disabled={selectedIds.length === 0 || send.isPending} onClick={() => send.mutate()}>
            {send.isPending ? "Sending…" : "Send enquiry"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

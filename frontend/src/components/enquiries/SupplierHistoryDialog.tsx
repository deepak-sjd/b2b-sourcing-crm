import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Star } from "lucide-react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/common/EmptyState";
import { Pagination } from "@/components/common/Pagination";
import { supplierEnquiriesApi } from "@/api/supplierEnquiries";
import { cn, formatDate } from "@/lib/utils";
import { SUPPLIER_REPLY_STATUS_LABELS } from "@/types/supplierEnquiry";

interface SupplierHistoryDialogProps {
  supplierId: string;
  supplierName: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

/** PRD 12.1: "Supplier history is kept, so the team can see all earlier
 *  enquiries, replies and prices from each supplier." */
export function SupplierHistoryDialog({ supplierId, supplierName, open, onOpenChange }: SupplierHistoryDialogProps) {
  const [page, setPage] = useState(0);

  const query = useQuery({
    queryKey: ["supplier-enquiries", "by-supplier", supplierId, page],
    queryFn: () => supplierEnquiriesApi.bySupplier(supplierId, page),
    enabled: open,
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>Enquiry history — {supplierName}</DialogTitle>
          <DialogDescription>Every price enquiry this supplier has been sent.</DialogDescription>
        </DialogHeader>

        {query.isLoading ? (
          <Skeleton className="h-48 w-full" />
        ) : !query.data || query.data.content.length === 0 ? (
          <EmptyState title="No enquiries yet" description="This supplier hasn't been sent a price enquiry." />
        ) : (
          <div className="space-y-2">
            <div className="max-h-[50vh] space-y-2 overflow-y-auto thin-scrollbar">
              {query.data.content.map((r) => (
                <div key={r.id} className="flex items-center justify-between gap-3 rounded-md border border-border p-2.5">
                  <div className="min-w-0">
                    <p className="flex items-center gap-1.5 truncate text-sm font-medium">
                      {r.chosen && <Star className="h-3.5 w-3.5 shrink-0 fill-amber-400 text-amber-400" />}
                      {r.itemDescription}
                      <span className="text-xs font-normal text-muted-foreground">{r.enquiryCode}</span>
                    </p>
                    <p className="truncate text-xs text-muted-foreground">
                      {r.leadName} · {r.repliedAt ? formatDate(r.repliedAt, true) : "Not replied yet"}
                    </p>
                  </div>
                  <div className="flex shrink-0 items-center gap-2">
                    {r.price != null && (
                      <span className="text-sm font-semibold tabular-nums">
                        {r.currency} {r.price.toLocaleString("en-IN")}
                      </span>
                    )}
                    <span
                      className={cn(
                        "rounded-full px-2 py-0.5 text-[11px] font-medium",
                        r.status === "REPLIED" && "bg-sky-100 text-sky-700",
                        r.status === "PENDING" && "bg-muted text-muted-foreground",
                        r.status === "DECLINED" && "bg-red-100 text-red-700",
                      )}
                    >
                      {SUPPLIER_REPLY_STATUS_LABELS[r.status]}
                    </span>
                  </div>
                </div>
              ))}
            </div>
            <Pagination
              page={query.data.page}
              totalPages={query.data.totalPages}
              totalElements={query.data.totalElements}
              onPageChange={setPage}
            />
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}

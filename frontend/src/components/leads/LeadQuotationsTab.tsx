import { useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { ComingSoon } from "@/components/common/ComingSoon";
import { QuotationStatusBadge } from "@/components/quotations/QuotationBadges";
import { quotationsApi } from "@/api/quotations";

export function LeadQuotationsTab({ leadId }: { leadId: string }) {
  const navigate = useNavigate();

  const quotationsQuery = useQuery({
    queryKey: ["quotations", "by-lead", leadId],
    queryFn: () => quotationsApi.byLead(leadId, 0, 10),
  });

  if (quotationsQuery.isLoading) {
    return (
      <div className="space-y-2">
        <Skeleton className="h-10 w-full" />
        <Skeleton className="h-10 w-full" />
      </div>
    );
  }

  const quotations = quotationsQuery.data?.content ?? [];
  const today = new Date().toISOString().slice(0, 10);

  return (
    <div>
      <div className="mb-3 flex justify-end">
        <Button size="sm" onClick={() => navigate(`/quotations/new?leadId=${leadId}`)}>
          <Plus className="h-4 w-4" />
          New quotation
        </Button>
      </div>

      {quotations.length === 0 ? (
        <ComingSoon
          title="No quotations yet"
          description="Quotations prepared for this lead — before it's even won — will show up here."
        />
      ) : (
        <div className="divide-y divide-border rounded-lg border border-border">
          {quotations.map((q) => {
            const expiringSoon = !!q.validUntil && q.validUntil < today;
            return (
              <button
                key={q.id}
                onClick={() => navigate(`/quotations/${q.id}/edit`)}
                className="flex w-full items-center justify-between gap-3 p-3 text-left text-sm hover:bg-accent/40"
              >
                <div>
                  <p className="font-medium">{q.quotationNumber}</p>
                  <p className="text-xs text-muted-foreground">
                    {q.quotationDate} · {q.lineItems.length} item(s)
                    {q.validUntil && (
                      <span className={expiringSoon ? "text-destructive" : undefined}>
                        {" "}
                        · valid till {q.validUntil}
                      </span>
                    )}
                  </p>
                </div>
                <div className="flex items-center gap-3">
                  <QuotationStatusBadge status={q.status} />
                  <div className="w-28 text-right">
                    <p className="font-medium tabular-nums">
                      {q.currency} {q.grandTotal.toFixed(2)}
                    </p>
                    {q.totalMarginAmount !== 0 && (
                      <p className="text-xs text-muted-foreground tabular-nums">
                        margin {q.currency} {q.totalMarginAmount.toFixed(2)}
                      </p>
                    )}
                  </div>
                </div>
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}

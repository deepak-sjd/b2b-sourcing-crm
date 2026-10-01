import { useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { ComingSoon } from "@/components/common/ComingSoon";
import { QuotationStatusBadge } from "@/components/quotations/QuotationBadges";
import { quotationsApi } from "@/api/quotations";

export function CustomerQuotationsTab({ customerId }: { customerId: string }) {
  const navigate = useNavigate();

  const quotationsQuery = useQuery({
    queryKey: ["quotations", "by-customer", customerId],
    queryFn: () => quotationsApi.byCustomer(customerId, 0, 10),
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

  return (
    <div>
      <div className="mb-3 flex justify-end">
        <Button size="sm" onClick={() => navigate(`/quotations/new?customerId=${customerId}`)}>
          <Plus className="h-4 w-4" />
          New quotation
        </Button>
      </div>

      {quotations.length === 0 ? (
        <ComingSoon
          title="No quotations yet"
          description="Quotations prepared for this customer will show up here."
        />
      ) : (
        <div className="divide-y divide-border rounded-lg border border-border">
          {quotations.map((q) => (
            <button
              key={q.id}
              onClick={() => navigate(`/quotations/${q.id}/edit`)}
              className="flex w-full items-center justify-between gap-3 p-3 text-left text-sm hover:bg-accent/40"
            >
              <div>
                <p className="font-medium">{q.quotationNumber}</p>
                <p className="text-xs text-muted-foreground">
                  {q.quotationDate} · {q.lineItems.length} item(s)
                </p>
              </div>
              <div className="flex items-center gap-3">
                <QuotationStatusBadge status={q.status} />
                <p className="w-24 text-right font-medium tabular-nums">
                  {q.currency} {q.grandTotal.toFixed(2)}
                </p>
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

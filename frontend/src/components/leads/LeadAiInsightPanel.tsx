import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { AlertTriangle, ArrowRight, RefreshCw, Sparkles } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { leadsApi } from "@/api/leads";
import { extractErrorMessage } from "@/api/client";
import { formatDate } from "@/lib/utils";
import { cn } from "@/lib/utils";
import type { LeadResponse } from "@/types/lead";

interface LeadAiInsightPanelProps {
  lead: LeadResponse;
  onUpdated: (lead: LeadResponse) => void;
}

/**
 * Surfaces the AI-generated summary and single next-best-action for this
 * lead. Loads automatically with the lead (both fields already come back on
 * GET /leads/{id}) — the "Refresh" button just re-runs generation on demand,
 * e.g. right after logging a call, without waiting for the nightly job.
 */
export function LeadAiInsightPanel({ lead, onUpdated }: LeadAiInsightPanelProps) {
  const queryClient = useQueryClient();

  const refresh = useMutation({
    mutationFn: () => leadsApi.refreshAiInsight(lead.id),
    onSuccess: (updated) => {
      onUpdated(updated);
      queryClient.invalidateQueries({ queryKey: ["leads"] });
      toast.success("AI insight refreshed");
    },
    onError: (error) => toast.error(extractErrorMessage(error, "Couldn't refresh AI insight")),
  });

  return (
    <Card className="border-primary/15 bg-gradient-to-br from-accent/60 to-card">
      <CardHeader className="flex-row items-center justify-between space-y-0">
        <CardTitle className="flex items-center gap-1.5 text-base font-semibold text-foreground">
          <Sparkles className="h-4 w-4 text-primary" />
          AI Insight
        </CardTitle>
        <Button
          variant="ghost"
          size="sm"
          className="h-7 gap-1.5 text-xs text-muted-foreground hover:text-foreground"
          disabled={refresh.isPending}
          onClick={() => refresh.mutate()}
        >
          <RefreshCw className={cn("h-3.5 w-3.5", refresh.isPending && "animate-spin")} />
          Refresh
        </Button>
      </CardHeader>
      <CardContent className="space-y-3">
        {lead.nextBestAction && (
          <div className="flex items-start gap-2 rounded-md border border-primary/20 bg-primary/5 p-2.5 text-sm font-medium text-primary">
            <ArrowRight className="mt-0.5 h-3.5 w-3.5 shrink-0" />
            <span>{lead.nextBestAction}</span>
          </div>
        )}

        {lead.aiSummary ? (
          <p className="text-sm leading-relaxed text-foreground">{lead.aiSummary}</p>
        ) : (
          <p className="text-sm text-muted-foreground">No AI summary generated for this lead yet.</p>
        )}

        {lead.atRisk && (
          <div className="flex items-start gap-2 rounded-md bg-amber-50 p-2.5 text-xs text-amber-800">
            <AlertTriangle className="mt-0.5 h-3.5 w-3.5 shrink-0" />
            <span>
              Inactive for {lead.daysSinceLastTouchpoint} days. Leads at this stage tend to drop off the longer
              they go silent — a follow-up is due.
            </span>
          </div>
        )}

        {lead.aiSummaryGeneratedAt && (
          <p className="text-[11px] text-muted-foreground">
            Last generated {formatDate(lead.aiSummaryGeneratedAt, true)} · refreshes nightly and on every update
          </p>
        )}
      </CardContent>
    </Card>
  );
}

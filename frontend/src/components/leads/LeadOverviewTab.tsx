import { Mail, MapPin, Phone, Tag, Wallet } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ScoreGauge } from "@/components/leads/ScoreGauge";
import { BuyingIntentBadge } from "@/components/leads/LeadBadges";
import { LeadAiInsightPanel } from "@/components/leads/LeadAiInsightPanel";
import { formatCurrencyINR, formatDate } from "@/lib/utils";
import type { LeadResponse } from "@/types/lead";

function Field({
  label,
  value,
  icon: Icon,
}: {
  label: string;
  value: React.ReactNode;
  icon?: React.ComponentType<{ className?: string }>;
}) {
  return (
    <div className="flex items-start gap-2.5">
      {Icon && <Icon className="mt-0.5 h-4 w-4 shrink-0 text-muted-foreground" />}
      <div className="min-w-0">
        <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">{label}</p>
        <p className="mt-0.5 truncate text-sm font-medium">{value ?? "—"}</p>
      </div>
    </div>
  );
}

interface LeadOverviewTabProps {
  lead: LeadResponse;
  /** Called after the AI Insight panel's Refresh button succeeds, so the
   *  parent page can sync its query cache (e.g. React Query's setQueryData). */
  onLeadUpdated?: (lead: LeadResponse) => void;
}

export function LeadOverviewTab({ lead, onLeadUpdated }: LeadOverviewTabProps) {
  return (
    <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
      <Card className="lg:col-span-2">
        <CardHeader className="flex-row items-center justify-between space-y-0">
          <CardTitle className="text-base font-semibold text-foreground">Contact &amp; deal details</CardTitle>
          <div className="flex items-center gap-2">
            {lead.buyingIntent && <BuyingIntentBadge intent={lead.buyingIntent} />}
            <ScoreGauge score={lead.score} size={44} strokeWidth={4} />
          </div>
        </CardHeader>
        <CardContent className="grid grid-cols-2 gap-x-4 gap-y-5 sm:grid-cols-3">
          <Field label="Phone" value={lead.phone} icon={Phone} />
          <Field label="Email" value={lead.email} icon={Mail} />
          <Field label="City" value={lead.city} icon={MapPin} />
          <Field label="Interest" value={lead.interest} icon={Tag} />
          <Field label="Budget" value={formatCurrencyINR(lead.budget)} icon={Wallet} />
          <Field label="Est. value" value={formatCurrencyINR(lead.estimatedValue)} icon={Wallet} />
          <Field label="Source" value={lead.source} />
          <Field label="Owner" value={lead.ownerName ?? "Unassigned"} />
          <Field
            label="Convert probability"
            value={lead.convertProbability != null ? `${lead.convertProbability}%` : "—"}
          />
          <Field label="Last touchpoint" value={formatDate(lead.lastTouchpointAt, true)} />
          <Field label="Next follow-up" value={formatDate(lead.nextFollowUpAt, true)} />
        </CardContent>
      </Card>

      <LeadAiInsightPanel lead={lead} onUpdated={(updated) => onLeadUpdated?.(updated)} />
    </div>
  );
}

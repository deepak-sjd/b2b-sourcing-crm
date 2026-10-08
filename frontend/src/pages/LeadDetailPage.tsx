import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";
import { Archive, ArrowLeft, Pencil, RotateCcw, UserPlus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { StageBadge, PriorityBadge, BuyingIntentBadge, LostReasonBadge, ScrapReasonBadge } from "@/components/leads/LeadBadges";
import { ScoreGauge } from "@/components/leads/ScoreGauge";
import { LeadOverviewTab } from "@/components/leads/LeadOverviewTab";
import { LeadFormDialog } from "@/components/leads/LeadFormDialog";
import { AssignOwnerDialog } from "@/components/leads/AssignOwnerDialog";
import { MarkWonDialog } from "@/components/leads/MarkWonDialog";
import { MarkLostDialog } from "@/components/leads/MarkLostDialog";
import { ScrapLeadDialog } from "@/components/leads/ScrapLeadDialog";
import { LeadTasksTab } from "@/components/tasks/LeadTasksTab";
import { LeadActivitiesTab } from "@/components/activities/LeadActivitiesTab";
import { LeadNotesTab } from "@/components/notes/LeadNotesTab";
import { LeadDocumentsTab } from "@/components/documents/LeadDocumentsTab";
import { LeadRequirementsTab } from "@/components/requirements/LeadRequirementsTab";
import { LeadSupplierEnquiriesTab } from "@/components/suppliers/LeadSupplierEnquiriesTab";
import { LeadQuotationsTab } from "@/components/leads/LeadQuotationsTab";
import { ConvertToCustomerButton } from "@/components/leads/ConvertToCustomerButton";
import { LeadAuditTab } from "@/components/leads/LeadAuditTab";
import { ComingSoon } from "@/components/common/ComingSoon";
import { useAuthStore } from "@/store/authStore";
import { leadsApi } from "@/api/leads";
import { extractErrorMessage } from "@/api/client";
import { formatCurrencyINR, formatDate } from "@/lib/utils";
import { CLOSED_STAGES, LEAD_STAGES, LEAD_STAGE_LABELS, type LeadResponse, type LeadStage } from "@/types/lead";

export function LeadDetailPage() {
  const { leadId } = useParams<{ leadId: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [editOpen, setEditOpen] = useState(false);
  const [assignOpen, setAssignOpen] = useState(false);
  const [wonOpen, setWonOpen] = useState(false);
  const [lostOpen, setLostOpen] = useState(false);
  const [scrapOpen, setScrapOpen] = useState(false);
  // Viewers are read-only (the backend rejects their writes too; this just hides the controls).
  const canWrite = useAuthStore((s) => s.user?.role) !== "VIEWER";

  const leadQuery = useQuery({
    queryKey: ["lead", leadId],
    queryFn: () => leadsApi.getById(leadId!),
    enabled: !!leadId,
  });

  const changeStage = useMutation({
    mutationFn: (stage: LeadStage) => leadsApi.changeStage(leadId!, stage),
    onSuccess: (updated) => {
      queryClient.setQueryData(["lead", leadId], updated);
      queryClient.invalidateQueries({ queryKey: ["leads"] });
      toast.success(`Moved to ${LEAD_STAGE_LABELS[updated.stage]}`);
    },
    onError: (error) => toast.error(extractErrorMessage(error, "Couldn't change stage")),
  });

  const reactivate = useMutation({
    mutationFn: () => leadsApi.reactivate(leadId!),
    onSuccess: (updated) => {
      queryClient.setQueryData(["lead", leadId], updated);
      queryClient.invalidateQueries({ queryKey: ["leads"] });
      toast.success(`${updated.fullName} reactivated from Scrap`);
    },
    onError: (error) => toast.error(extractErrorMessage(error, "Couldn't reactivate this lead")),
  });

  /** Won/Lost need extra data the plain stage dropdown can't collect, so
   *  picking either one here opens the matching dialog instead of firing
   *  changeStage directly - see MarkWonDialog/MarkLostDialog. */
  function handleStageSelect(value: string) {
    if (value === "WON") {
      setWonOpen(true);
      return;
    }
    if (value === "LOST") {
      setLostOpen(true);
      return;
    }
    changeStage.mutate(value as LeadStage);
  }

  function syncLead(updated: LeadResponse) {
    queryClient.setQueryData(["lead", leadId], updated);
    queryClient.invalidateQueries({ queryKey: ["leads"] });
  }

  // Shared by the AI Insight panel's "Refresh" button (both on the Overview
  // tab and the AI tab) so a refresh there updates this page's cached lead
  // immediately, the same way changeStage does above.
  function handleLeadUpdated(updated: typeof leadQuery.data) {
    if (!updated) return;
    syncLead(updated);
  }

  if (leadQuery.isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-40 w-full" />
      </div>
    );
  }

  if (leadQuery.isError || !leadQuery.data) {
    const notFound = axios.isAxiosError(leadQuery.error) && leadQuery.error.response?.status === 404;
    return (
      <div>
        <Button variant="ghost" size="sm" className="mb-3 -ml-2 text-muted-foreground" onClick={() => navigate("/leads")}>
          <ArrowLeft className="h-4 w-4" />
          Back to leads
        </Button>
        {notFound ? (
          <ComingSoon title="Lead not found" description="This lead doesn't exist, or you don't have access to it." />
        ) : (
          <div className="rounded-lg border border-border p-6 text-center">
            <p className="font-medium">Couldn't load this lead</p>
            <p className="mt-1 text-sm text-muted-foreground">
              {extractErrorMessage(leadQuery.error, "Please check your connection and try again.")}
            </p>
            <Button className="mt-4" variant="outline" size="sm" onClick={() => leadQuery.refetch()}>
              Try again
            </Button>
          </div>
        )}
      </div>
    );
  }

  const lead = leadQuery.data;
  const stageLocked = CLOSED_STAGES.includes(lead.stage) || lead.scrapped;

  return (
    <div>
      <Button variant="ghost" size="sm" className="mb-3 -ml-2 text-muted-foreground" onClick={() => navigate("/leads")}>
        <ArrowLeft className="h-4 w-4" />
        Back to leads
      </Button>

      {/* PRD §14.3 - scrapped leads stay visible and fully intact, just
          flagged, with a Reactivate path straight back into the funnel. */}
      {lead.scrapped && (
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3 rounded-lg border border-border bg-muted/60 p-4 text-sm">
          <div>
            <p className="font-medium">
              In Scrap{lead.scrapReason && <> · <ScrapReasonBadge reason={lead.scrapReason} /></>}
            </p>
            <p className="mt-0.5 text-muted-foreground">
              {lead.scrapRemarks ?? "No remarks."} {lead.scrappedAt && <>Moved on {formatDate(lead.scrappedAt)}.</>}
            </p>
          </div>
          {canWrite && (
            <Button size="sm" variant="outline" onClick={() => reactivate.mutate()} disabled={reactivate.isPending}>
              <RotateCcw className="h-4 w-4" />
              {reactivate.isPending ? "Reactivating…" : "Reactivate"}
            </Button>
          )}
        </div>
      )}

      {/* PRD §14.1/§14.2 - the reason/value trail behind a closed lead. */}
      {(lead.stage === "WON" || lead.stage === "LOST") && (
        <div className="mb-4 rounded-lg border border-border bg-muted/40 p-4 text-sm">
          {lead.stage === "WON" ? (
            <p>
              <span className="font-medium">Closed Won</span>
              {lead.finalValue != null && <> · Final value {formatCurrencyINR(lead.finalValue)}</>}
              {lead.closedAt && <> · {formatDate(lead.closedAt)}</>}
            </p>
          ) : (
            <p className="flex flex-wrap items-center gap-1.5">
              <span className="font-medium">Closed Lost</span>
              {lead.lostReason && <LostReasonBadge reason={lead.lostReason} />}
              {lead.closedAt && <>· {formatDate(lead.closedAt)}</>}
            </p>
          )}
          {(lead.closureRemarks || lead.lostComment) && (
            <p className="mt-1 text-muted-foreground">{lead.closureRemarks ?? lead.lostComment}</p>
          )}
        </div>
      )}

      <div className="mb-5 flex flex-wrap items-start justify-between gap-4 rounded-lg border border-border bg-gradient-to-r from-accent/50 to-card p-4">
        <div className="flex items-start gap-3.5">
          <ScoreGauge score={lead.score} size={52} strokeWidth={5} />
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <h1 className="text-xl font-semibold tracking-tight">{lead.fullName}</h1>
              <PriorityBadge priority={lead.priority} />
              {lead.buyingIntent && <BuyingIntentBadge intent={lead.buyingIntent} />}
            </div>
            <p className="mt-0.5 text-sm text-muted-foreground">
              {lead.interest ?? "No requirement set"} · {lead.city ?? "No city set"} · Owner:{" "}
              {lead.ownerName ?? "Unassigned"}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <Select value={lead.stage} disabled={!canWrite || stageLocked || changeStage.isPending} onValueChange={handleStageSelect}>
            <SelectTrigger className="w-44">
              <SelectValue>
                <StageBadge stage={lead.stage} />
              </SelectValue>
            </SelectTrigger>
            <SelectContent>
              {LEAD_STAGES.map((s) => (
                <SelectItem key={s} value={s} disabled={CLOSED_STAGES.includes(s) && s !== lead.stage}>
                  {LEAD_STAGE_LABELS[s]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          {canWrite && (
            <>
              <Button variant="outline" onClick={() => setAssignOpen(true)} disabled={lead.scrapped}>
                <UserPlus className="h-4 w-4" />
                Assign
              </Button>
              <Button variant="outline" onClick={() => setEditOpen(true)}>
                <Pencil className="h-4 w-4" />
                Edit
              </Button>
              <ConvertToCustomerButton lead={lead} />
            </>
          )}
          {canWrite && !lead.scrapped && lead.stage !== "WON" && lead.stage !== "LOST" && (
            <Button variant="outline" onClick={() => setScrapOpen(true)}>
              <Archive className="h-4 w-4" />
              Scrap
            </Button>
          )}
        </div>
      </div>

      <Tabs defaultValue="overview">
        <TabsList className="max-w-full">
          <TabsTrigger value="overview">Overview</TabsTrigger>
          <TabsTrigger value="timeline">Timeline</TabsTrigger>
          <TabsTrigger value="requirements">Requirements</TabsTrigger>
          <TabsTrigger value="supplier-enquiries">Supplier Enquiries</TabsTrigger>
          <TabsTrigger value="quotations">Quotations</TabsTrigger>
          <TabsTrigger value="deals">Deals</TabsTrigger>
          <TabsTrigger value="tasks">Tasks</TabsTrigger>
          <TabsTrigger value="calls">Activities</TabsTrigger>
          <TabsTrigger value="emails">Emails</TabsTrigger>
          <TabsTrigger value="whatsapp">WhatsApp</TabsTrigger>
          <TabsTrigger value="meetings">Meetings</TabsTrigger>
          <TabsTrigger value="notes">Notes</TabsTrigger>
          <TabsTrigger value="documents">Documents</TabsTrigger>
          <TabsTrigger value="invoices">Invoices</TabsTrigger>
          <TabsTrigger value="audit">Audit Logs</TabsTrigger>
          <TabsTrigger value="automation">Automation</TabsTrigger>
          <TabsTrigger value="ai">AI</TabsTrigger>
          <TabsTrigger value="support">Support Tickets</TabsTrigger>
          <TabsTrigger value="contacts">Related Contacts</TabsTrigger>
          <TabsTrigger value="companies">Related Companies</TabsTrigger>
        </TabsList>

        <TabsContent value="overview">
          <LeadOverviewTab lead={lead} onLeadUpdated={handleLeadUpdated} />
        </TabsContent>
        <TabsContent value="timeline">
          <ComingSoon
            title="Unified activity timeline"
            description="A merged, chronological feed of every call, email, WhatsApp message, and stage change for this lead."
            phase="Phase 2"
          />
        </TabsContent>
        <TabsContent value="requirements">
          <LeadRequirementsTab leadId={lead.id} />
        </TabsContent>
        <TabsContent value="supplier-enquiries">
          <LeadSupplierEnquiriesTab leadId={lead.id} />
        </TabsContent>
        <TabsContent value="quotations">
          <LeadQuotationsTab leadId={lead.id} />
        </TabsContent>
        <TabsContent value="deals">
          <ComingSoon
            title="Deals"
            description="Track enrollment/payment deals tied to this lead once the Deals module ships."
            phase="Phase 3"
          />
        </TabsContent>
        <TabsContent value="tasks">
          <LeadTasksTab leadId={lead.id} />
        </TabsContent>
        <TabsContent value="calls">
          <LeadActivitiesTab leadId={lead.id} />
        </TabsContent>
        <TabsContent value="emails">
          <ComingSoon title="Email thread" description="Two-way email sync isn't wired up yet." phase="Phase 2" />
        </TabsContent>
        <TabsContent value="whatsapp">
          <ComingSoon
            title="WhatsApp conversation"
            description="WhatsApp Business API integration for this lead."
            phase="Phase 2"
          />
        </TabsContent>
        <TabsContent value="meetings">
          <ComingSoon title="Meetings" description="Scheduled customer meetings." phase="Phase 2" />
        </TabsContent>
        <TabsContent value="notes">
          <LeadNotesTab leadId={lead.id} />
        </TabsContent>
        <TabsContent value="documents">
          <LeadDocumentsTab leadId={lead.id} />
        </TabsContent>
        <TabsContent value="invoices">
          <ComingSoon title="Invoices" description="Payment plans and invoices for this lead." phase="Phase 3" />
        </TabsContent>
        <TabsContent value="audit">
          <LeadAuditTab leadId={lead.id} />
        </TabsContent>
        <TabsContent value="automation">
          <ComingSoon title="Automation" description="Workflow rules triggered by this lead's activity." phase="Phase 3" />
        </TabsContent>
        <TabsContent value="ai">
          <div className="space-y-4">
            <LeadOverviewTab lead={lead} onLeadUpdated={handleLeadUpdated} />
            <ComingSoon
              title="Next best actions & suggested outreach"
              description="AI-generated action recommendations and outreach drafts, beyond the summary shown above."
              phase="Phase 2"
            />
          </div>
        </TabsContent>
        <TabsContent value="support">
          <ComingSoon title="Support tickets" description="No support tickets integration yet." phase="Phase 3" />
        </TabsContent>
        <TabsContent value="contacts">
          <ComingSoon title="Related contacts" description="Parents, guardians, or other contacts linked to this lead." phase="Phase 2" />
        </TabsContent>
        <TabsContent value="companies">
          <ComingSoon title="Related companies" description="Organizations associated with this lead (e.g. corporate tie-ups)." phase="Phase 3" />
        </TabsContent>
      </Tabs>

      <LeadFormDialog open={editOpen} onOpenChange={setEditOpen} lead={lead} onSaved={syncLead} />
      <AssignOwnerDialog open={assignOpen} onOpenChange={setAssignOpen} lead={lead} onAssigned={syncLead} />
      <MarkWonDialog open={wonOpen} onOpenChange={setWonOpen} lead={lead} onClosed={syncLead} />
      <MarkLostDialog open={lostOpen} onOpenChange={setLostOpen} lead={lead} onClosed={syncLead} />
      <ScrapLeadDialog open={scrapOpen} onOpenChange={setScrapOpen} lead={lead} onScrapped={syncLead} />
    </div>
  );
}

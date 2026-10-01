import { useMemo, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { PriorityBadge, ScoreBadge } from "@/components/leads/LeadBadges";
import { leadsApi } from "@/api/leads";
import { extractErrorMessage } from "@/api/client";
import { cn, formatRelativeDay, initials } from "@/lib/utils";
import { CLOSED_STAGES, LEAD_STAGES, LEAD_STAGE_LABELS, type LeadResponse, type LeadStage } from "@/types/lead";

const STAGE_BG: Record<LeadStage, string> = {
  NEW: "bg-stage-new",
  CONTACTED: "bg-stage-contacted",
  QUALIFIED: "bg-stage-qualified",
  MEETING: "bg-stage-meeting",
  OPPORTUNITY: "bg-stage-opportunity",
  QUOTATION: "bg-stage-quotation",
  WON: "bg-stage-won",
  LOST: "bg-stage-lost",
};

interface LeadBoardViewProps {
  leads: LeadResponse[];
  onOpenLead: (id: string) => void;
}

/** Drag-and-drop kanban, grouped by pipeline stage. Uses native HTML5 DnD
 *  (no extra dependency) — drag a card onto another column to call the same
 *  PATCH /leads/{id}/stage the detail page's stage selector uses, so backend
 *  validation (e.g. "can't move out of a closed stage") is enforced identically. */
export function LeadBoardView({ leads, onOpenLead }: LeadBoardViewProps) {
  const queryClient = useQueryClient();
  const [dragOverStage, setDragOverStage] = useState<LeadStage | null>(null);

  const columns = useMemo(() => {
    const grouped = new Map<LeadStage, LeadResponse[]>(LEAD_STAGES.map((s) => [s, []]));
    for (const lead of leads) grouped.get(lead.stage)?.push(lead);
    return grouped;
  }, [leads]);

  const changeStage = useMutation({
    mutationFn: ({ id, stage }: { id: string; stage: LeadStage }) => leadsApi.changeStage(id, stage),
    onSuccess: (updated) => {
      queryClient.invalidateQueries({ queryKey: ["leads"] });
      queryClient.setQueryData(["lead", updated.id], updated);
      toast.success(`Moved to ${LEAD_STAGE_LABELS[updated.stage]}`);
    },
    onError: (error) => toast.error(extractErrorMessage(error, "Couldn't move this lead")),
  });

  function handleDrop(stage: LeadStage, e: React.DragEvent) {
    e.preventDefault();
    setDragOverStage(null);
    const id = e.dataTransfer.getData("text/lead-id");
    const fromStage = e.dataTransfer.getData("text/lead-stage") as LeadStage;
    if (!id || fromStage === stage) return;
    changeStage.mutate({ id, stage });
  }

  return (
    <div className="flex gap-3 overflow-x-auto pb-2 thin-scrollbar">
      {LEAD_STAGES.map((stage) => {
        const stageLeads = columns.get(stage) ?? [];
        return (
          <div
            key={stage}
            className={cn(
              "flex w-72 shrink-0 flex-col rounded-lg border border-border bg-muted/40 transition-colors",
              dragOverStage === stage && "border-primary/50 bg-primary/5",
            )}
            onDragOver={(e) => {
              e.preventDefault();
              setDragOverStage(stage);
            }}
            onDragLeave={() => setDragOverStage((s) => (s === stage ? null : s))}
            onDrop={(e) => handleDrop(stage, e)}
          >
            <div className="flex items-center gap-2 border-b border-border px-3 py-2.5">
              <span className={cn("h-2 w-2 rounded-full", STAGE_BG[stage])} />
              <span className="text-sm font-semibold">{LEAD_STAGE_LABELS[stage]}</span>
              <span className="ml-auto rounded-full bg-background px-1.5 text-xs text-muted-foreground">
                {stageLeads.length}
              </span>
            </div>

            <div className="flex flex-1 flex-col gap-2 p-2">
              {stageLeads.length === 0 && (
                <p className="px-2 py-6 text-center text-xs text-muted-foreground">No leads</p>
              )}
              {stageLeads.map((lead) => {
                const draggable = !CLOSED_STAGES.includes(lead.stage);
                return (
                  <div
                    key={lead.id}
                    role="button"
                    tabIndex={0}
                    draggable={draggable}
                    onDragStart={(e) => {
                      e.dataTransfer.setData("text/lead-id", lead.id);
                      e.dataTransfer.setData("text/lead-stage", lead.stage);
                    }}
                    onClick={() => onOpenLead(lead.id)}
                    className={cn(
                      "cursor-pointer rounded-md border border-border bg-card p-2.5 shadow-sm transition-shadow hover:shadow-md",
                      draggable && "cursor-grab active:cursor-grabbing",
                    )}
                  >
                    <div className="flex items-start justify-between gap-2">
                      <p className="truncate text-sm font-medium">{lead.fullName}</p>
                      <ScoreBadge score={lead.score} />
                    </div>
                    <p className="mt-0.5 truncate text-xs text-muted-foreground">{lead.interest ?? "—"}</p>
                    <div className="mt-2 flex items-center justify-between">
                      <PriorityBadge priority={lead.priority} />
                      <div className="flex items-center gap-1.5">
                        {lead.atRisk && <span className="h-1.5 w-1.5 rounded-full bg-amber-500" title="At risk" />}
                        <span className="text-[11px] text-muted-foreground">
                          {formatRelativeDay(lead.nextFollowUpAt)}
                        </span>
                        <Avatar className="h-5 w-5">
                          <AvatarFallback className="text-[9px]">{initials(lead.ownerName)}</AvatarFallback>
                        </Avatar>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        );
      })}
    </div>
  );
}

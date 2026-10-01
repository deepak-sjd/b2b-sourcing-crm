import { useMemo, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { AlertTriangle, LayoutGrid, Plus, Rows3, Search, Users2 } from "lucide-react";
import { PageHeader } from "@/components/common/PageHeader";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/common/EmptyState";
import { Pagination } from "@/components/common/Pagination";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from "@/components/ui/table";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { StageBadge, PriorityBadge, ScoreBadge, BuyingIntentBadge } from "@/components/leads/LeadBadges";
import { LeadFormDialog } from "@/components/leads/LeadFormDialog";
import { LeadStatsBar } from "@/components/leads/LeadStatsBar";
import { LeadBoardView } from "@/components/leads/LeadBoardView";
import { leadsApi } from "@/api/leads";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import { cn, formatRelativeDay, initials } from "@/lib/utils";
import { LEAD_STAGES, LEAD_STAGE_LABELS, type LeadStage } from "@/types/lead";

const PAGE_SIZE = 20;
const BOARD_SIZE = 300; // Board view groups by stage client-side, so it needs the wider pool, not one page.

export function LeadsPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [page, setPage] = useState(0);
  const [stage, setStage] = useState<LeadStage | "ALL">("ALL");
  const [search, setSearch] = useState("");
  const debouncedSearch = useDebouncedValue(search, 250);
  const [createOpen, setCreateOpen] = useState(false);
  const [view, setView] = useState<"table" | "board">("table");

  const leadsQuery = useQuery({
    queryKey: ["leads", { page, stage }],
    queryFn: () => leadsApi.list({ page, size: PAGE_SIZE, stage: stage === "ALL" ? undefined : stage }),
  });

  const boardQuery = useQuery({
    queryKey: ["leads", "board"],
    queryFn: () => leadsApi.list({ page: 0, size: BOARD_SIZE }),
    enabled: view === "board",
  });

  // Was already available on the backend (GET /leads/at-risk) but never called
  // anywhere in the UI — now powers both the "At risk" stat card and the
  // amber dot next to a lead's follow-up date.
  const atRiskQuery = useQuery({
    queryKey: ["leads", "at-risk"],
    queryFn: () => leadsApi.atRisk(),
  });
  const atRiskIds = useMemo(() => new Set(atRiskQuery.data?.map((l) => l.id) ?? []), [atRiskQuery.data]);

  // The backend's GET /leads has no free-text search param (see LeadRepository —
  // deliberately no ad-hoc query methods). Until a real search endpoint exists,
  // this filters only the leads already loaded on the current page.
  const visibleLeads = useMemo(() => {
    const content = leadsQuery.data?.content ?? [];
    const q = debouncedSearch.trim().toLowerCase();
    if (!q) return content;
    return content.filter(
      (lead) =>
        lead.fullName.toLowerCase().includes(q) ||
        lead.phone?.toLowerCase().includes(q) ||
        lead.email?.toLowerCase().includes(q),
    );
  }, [leadsQuery.data, debouncedSearch]);

  const hotCount = leadsQuery.data?.content.filter((l) => l.buyingIntent === "HOT").length ?? 0;
  const avgScore = leadsQuery.data?.content.length
    ? Math.round(leadsQuery.data.content.reduce((sum, l) => sum + l.score, 0) / leadsQuery.data.content.length)
    : 0;

  return (
    <div>
      <PageHeader
        title="Leads"
        description="Your pipeline, one row per lead."
        actions={
          <Button onClick={() => setCreateOpen(true)}>
            <Plus className="h-4 w-4" />
            New Lead
          </Button>
        }
      />

      <LeadStatsBar
        totalElements={leadsQuery.data?.totalElements ?? 0}
        hotCount={hotCount}
        atRiskCount={atRiskQuery.data?.length ?? 0}
        avgScore={avgScore}
        loading={leadsQuery.isLoading}
      />

      <Card>
        <div className="flex flex-wrap items-center gap-2 border-b border-border p-3">
          <div className="relative w-full max-w-xs">
            <Search className="pointer-events-none absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Filter this page by name, phone, email…"
              className="pl-8"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
          <Select
            value={stage}
            onValueChange={(v) => {
              setStage(v as LeadStage | "ALL");
              setPage(0);
            }}
          >
            <SelectTrigger className="w-40">
              <SelectValue placeholder="Stage" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">All stages</SelectItem>
              {LEAD_STAGES.map((s) => (
                <SelectItem key={s} value={s}>
                  {LEAD_STAGE_LABELS[s]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>

          <div className="ml-auto flex items-center gap-1 rounded-md border border-border p-0.5">
            <Button
              variant={view === "table" ? "secondary" : "ghost"}
              size="sm"
              className="h-7 px-2.5"
              onClick={() => setView("table")}
            >
              <Rows3 className="h-3.5 w-3.5" />
              Table
            </Button>
            <Button
              variant={view === "board" ? "secondary" : "ghost"}
              size="sm"
              className="h-7 px-2.5"
              onClick={() => setView("board")}
            >
              <LayoutGrid className="h-3.5 w-3.5" />
              Board
            </Button>
          </div>
        </div>

        {view === "board" ? (
          boardQuery.isLoading ? (
            <div className="flex gap-3 p-4">
              {Array.from({ length: 4 }).map((_, i) => (
                <Skeleton key={i} className="h-64 w-72 shrink-0" />
              ))}
            </div>
          ) : (
            <div className="p-3">
              <LeadBoardView leads={boardQuery.data?.content ?? []} onOpenLead={(id) => navigate(`/leads/${id}`)} />
            </div>
          )
        ) : leadsQuery.isLoading ? (
          <div className="space-y-2 p-4">
            {Array.from({ length: 6 }).map((_, i) => (
              <Skeleton key={i} className="h-10 w-full" />
            ))}
          </div>
        ) : visibleLeads.length === 0 ? (
          <EmptyState
            icon={Users2}
            title="No leads to show"
            description="Try a different stage filter, or create your first lead."
            action={
              <Button size="sm" className="mt-1" onClick={() => setCreateOpen(true)}>
                <Plus className="h-4 w-4" />
                New Lead
              </Button>
            }
          />
        ) : (
          <>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Name</TableHead>
                  <TableHead>Stage</TableHead>
                  <TableHead>Priority</TableHead>
                  <TableHead>Score</TableHead>
                  <TableHead>Intent</TableHead>
                  <TableHead>Next follow-up</TableHead>
                  <TableHead>Owner</TableHead>
                  <TableHead>Source</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {visibleLeads.map((lead) => (
                  <TableRow
                    key={lead.id}
                    className="cursor-pointer"
                    onClick={() => navigate(`/leads/${lead.id}`)}
                  >
                    <TableCell>
                      <p className="font-medium">{lead.fullName}</p>
                      <p className="text-xs text-muted-foreground">{lead.interest ?? "—"}</p>
                    </TableCell>
                    <TableCell>
                      <StageBadge stage={lead.stage} />
                    </TableCell>
                    <TableCell>
                      <PriorityBadge priority={lead.priority} />
                    </TableCell>
                    <TableCell>
                      <ScoreBadge score={lead.score} />
                    </TableCell>
                    <TableCell>{lead.buyingIntent && <BuyingIntentBadge intent={lead.buyingIntent} />}</TableCell>
                    <TableCell>
                      <span className={cn("inline-flex items-center gap-1.5 text-muted-foreground")}>
                        {atRiskIds.has(lead.id) && (
                          <AlertTriangle className="h-3 w-3 shrink-0 text-amber-500" aria-label="At risk" />
                        )}
                        {formatRelativeDay(lead.nextFollowUpAt)}
                      </span>
                    </TableCell>
                    <TableCell>
                      <span className="flex items-center gap-2 text-muted-foreground">
                        <Avatar className="h-6 w-6">
                          <AvatarFallback className="text-[10px]">{initials(lead.ownerName)}</AvatarFallback>
                        </Avatar>
                        {lead.ownerName ?? "Unassigned"}
                      </span>
                    </TableCell>
                    <TableCell className="text-muted-foreground">{lead.source ?? "—"}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            {leadsQuery.data && (
              <Pagination
                page={leadsQuery.data.page}
                totalPages={leadsQuery.data.totalPages}
                totalElements={leadsQuery.data.totalElements}
                onPageChange={setPage}
              />
            )}
          </>
        )}
      </Card>

      <LeadFormDialog
        open={createOpen}
        onOpenChange={setCreateOpen}
        onSaved={() => {
          queryClient.invalidateQueries({ queryKey: ["leads"] });
        }}
      />
    </div>
  );
}

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { Archive, RotateCcw } from "lucide-react";
import { PageHeader } from "@/components/common/PageHeader";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/common/EmptyState";
import { Pagination } from "@/components/common/Pagination";
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from "@/components/ui/table";
import { ScrapReasonBadge, StageBadge } from "@/components/leads/LeadBadges";
import { leadsApi } from "@/api/leads";
import { extractErrorMessage } from "@/api/client";
import { formatDate } from "@/lib/utils";

const PAGE_SIZE = 20;

/** PRD §14.3: "Authorised users can search and review Scrap records. A Scrap
 *  record can be reactivated if the customer becomes relevant again." A
 *  deliberately separate page from LeadsPage - scrapped leads never appear
 *  in the normal pipeline views (see LeadService.notScrapped), so this is
 *  the only place they're visible at all. */
export function ScrapPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);

  const scrapQuery = useQuery({
    queryKey: ["leads", "scrap", { page }],
    queryFn: () => leadsApi.listScrapped(page, PAGE_SIZE),
  });

  const reactivate = useMutation({
    mutationFn: (id: string) => leadsApi.reactivate(id),
    onSuccess: (updated) => {
      toast.success(`${updated.fullName} reactivated from Scrap`);
      queryClient.invalidateQueries({ queryKey: ["leads"] });
    },
    onError: (error) => toast.error(extractErrorMessage(error, "Couldn't reactivate this lead")),
  });

  const leads = scrapQuery.data?.content ?? [];

  return (
    <div>
      <PageHeader
        title="Scrap Records"
        description="Leads moved to Scrap - archived, not deleted. Reactivate any of these back into the pipeline."
      />

      <Card>
        {scrapQuery.isLoading ? (
          <div className="space-y-2 p-4">
            {Array.from({ length: 6 }).map((_, i) => (
              <Skeleton key={i} className="h-10 w-full" />
            ))}
          </div>
        ) : leads.length === 0 ? (
          <EmptyState
            icon={Archive}
            title="Scrap is empty"
            description="Leads moved to Scrap from a lead's detail page will show up here."
          />
        ) : (
          <>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Name</TableHead>
                  <TableHead>Stage when scrapped</TableHead>
                  <TableHead>Reason</TableHead>
                  <TableHead>Remarks</TableHead>
                  <TableHead>Scrapped on</TableHead>
                  <TableHead className="text-right">Action</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {leads.map((lead) => (
                  <TableRow key={lead.id} className="cursor-pointer" onClick={() => navigate(`/leads/${lead.id}`)}>
                    <TableCell>
                      <p className="font-medium">{lead.fullName}</p>
                      <p className="text-xs text-muted-foreground">{lead.interest ?? "—"}</p>
                    </TableCell>
                    <TableCell>
                      <StageBadge stage={lead.stage} />
                    </TableCell>
                    <TableCell>{lead.scrapReason && <ScrapReasonBadge reason={lead.scrapReason} />}</TableCell>
                    <TableCell className="max-w-xs truncate text-muted-foreground">
                      {lead.scrapRemarks ?? "—"}
                    </TableCell>
                    <TableCell className="text-muted-foreground">{formatDate(lead.scrappedAt)}</TableCell>
                    <TableCell className="text-right">
                      <Button
                        size="sm"
                        variant="outline"
                        disabled={reactivate.isPending}
                        onClick={(e) => {
                          e.stopPropagation();
                          reactivate.mutate(lead.id);
                        }}
                      >
                        <RotateCcw className="h-4 w-4" />
                        Reactivate
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            {scrapQuery.data && (
              <Pagination
                page={scrapQuery.data.page}
                totalPages={scrapQuery.data.totalPages}
                totalElements={scrapQuery.data.totalElements}
                onPageChange={setPage}
              />
            )}
          </>
        )}
      </Card>
    </div>
  );
}

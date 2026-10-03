import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { leadsApi } from "@/api/leads";
import { extractErrorMessage } from "@/api/client";
import { SCRAP_REASONS, SCRAP_REASON_LABELS, type LeadResponse, type ScrapReason } from "@/types/lead";

interface ScrapLeadDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  lead: LeadResponse;
  onScrapped: (lead: LeadResponse) => void;
}

/** PRD §14.3 - "Scrap means archived. It does NOT mean permanently deleted."
 *  Reason is required (the PRD lists it as information always kept on the
 *  record); remarks are free text on top. */
export function ScrapLeadDialog({ open, onOpenChange, lead, onScrapped }: ScrapLeadDialogProps) {
  const [scrapReason, setScrapReason] = useState<ScrapReason | "">("");
  const [remarks, setRemarks] = useState("");
  const queryClient = useQueryClient();

  const scrap = useMutation({
    mutationFn: () => {
      if (!scrapReason) throw new Error("Pick a reason first");
      return leadsApi.scrap(lead.id, { scrapReason, remarks: remarks.trim() || null });
    },
    onSuccess: (updated) => {
      toast.success(`${updated.fullName} moved to Scrap`);
      queryClient.invalidateQueries({ queryKey: ["leads"] });
      onScrapped(updated);
      onOpenChange(false);
      setScrapReason("");
      setRemarks("");
    },
    onError: (error) => toast.error(extractErrorMessage(error, "Couldn't move this lead to Scrap")),
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Move {lead.fullName} to Scrap</DialogTitle>
          <DialogDescription>
            Archived, not deleted — everything stays on the record and this can be reactivated later.
          </DialogDescription>
        </DialogHeader>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            if (scrapReason) scrap.mutate();
          }}
          className="space-y-3"
        >
          <div className="space-y-1.5">
            <Label>Reason</Label>
            <Select value={scrapReason} onValueChange={(v) => setScrapReason(v as ScrapReason)}>
              <SelectTrigger>
                <SelectValue placeholder="Why is this going to Scrap?" />
              </SelectTrigger>
              <SelectContent>
                {SCRAP_REASONS.map((r) => (
                  <SelectItem key={r} value={r}>
                    {SCRAP_REASON_LABELS[r]}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="scrapRemarks">Remarks (optional)</Label>
            <Textarea
              id="scrapRemarks"
              rows={3}
              value={remarks}
              onChange={(e) => setRemarks(e.target.value)}
            />
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" variant="destructive" disabled={!scrapReason || scrap.isPending}>
              {scrap.isPending ? "Moving…" : "Move to Scrap"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

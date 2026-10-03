import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { leadsApi } from "@/api/leads";
import { extractErrorMessage } from "@/api/client";
import type { LeadResponse } from "@/types/lead";

interface MarkWonDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  lead: LeadResponse;
  onClosed: (lead: LeadResponse) => void;
}

/** PRD §14.1 - unlike Lost, neither field is required to close a lead as
 *  Won; both are optional context for the record. */
export function MarkWonDialog({ open, onOpenChange, lead, onClosed }: MarkWonDialogProps) {
  const [finalValue, setFinalValue] = useState("");
  const [remarks, setRemarks] = useState("");
  const queryClient = useQueryClient();

  const markWon = useMutation({
    mutationFn: () =>
      leadsApi.markWon(lead.id, {
        finalValue: finalValue.trim() ? Number(finalValue) : null,
        remarks: remarks.trim() || null,
      }),
    onSuccess: (updated) => {
      toast.success(`${updated.fullName} marked Won`);
      queryClient.invalidateQueries({ queryKey: ["leads"] });
      onClosed(updated);
      onOpenChange(false);
      setFinalValue("");
      setRemarks("");
    },
    onError: (error) => toast.error(extractErrorMessage(error, "Couldn't mark this lead as Won")),
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Mark {lead.fullName} as Won</DialogTitle>
          <DialogDescription>This closes the lead and can't be undone from here.</DialogDescription>
        </DialogHeader>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            markWon.mutate();
          }}
          className="space-y-3"
        >
          <div className="space-y-1.5">
            <Label htmlFor="finalValue">Final value (optional)</Label>
            <Input
              id="finalValue"
              type="number"
              inputMode="decimal"
              placeholder={lead.estimatedValue != null ? `Estimated: ${lead.estimatedValue}` : "e.g. 45000"}
              value={finalValue}
              onChange={(e) => setFinalValue(e.target.value)}
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="closureRemarks">Remarks (optional)</Label>
            <Textarea
              id="closureRemarks"
              rows={3}
              value={remarks}
              onChange={(e) => setRemarks(e.target.value)}
              placeholder="Anything worth recording about how this closed"
            />
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={markWon.isPending}>
              {markWon.isPending ? "Saving…" : "Mark Won"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

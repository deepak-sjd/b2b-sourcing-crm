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
import { LOST_REASONS, LOST_REASON_LABELS, type LeadResponse, type LostReason } from "@/types/lead";

interface MarkLostDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  lead: LeadResponse;
  onClosed: (lead: LeadResponse) => void;
}

/** PRD §14.2: "A reason is required, and the user can add a comment." -
 *  Submit stays disabled until a reason is picked; the backend enforces the
 *  same constraint independently (MarkLostRequest.lostReason is @NotNull). */
export function MarkLostDialog({ open, onOpenChange, lead, onClosed }: MarkLostDialogProps) {
  const [lostReason, setLostReason] = useState<LostReason | "">("");
  const [comment, setComment] = useState("");
  const queryClient = useQueryClient();

  const markLost = useMutation({
    mutationFn: () => {
      if (!lostReason) throw new Error("Pick a reason first");
      return leadsApi.markLost(lead.id, { lostReason, comment: comment.trim() || null });
    },
    onSuccess: (updated) => {
      toast.success(`${updated.fullName} marked Lost`);
      queryClient.invalidateQueries({ queryKey: ["leads"] });
      onClosed(updated);
      onOpenChange(false);
      setLostReason("");
      setComment("");
    },
    onError: (error) => toast.error(extractErrorMessage(error, "Couldn't mark this lead as Lost")),
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Mark {lead.fullName} as Lost</DialogTitle>
          <DialogDescription>This closes the lead and can't be undone from here.</DialogDescription>
        </DialogHeader>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            if (lostReason) markLost.mutate();
          }}
          className="space-y-3"
        >
          <div className="space-y-1.5">
            <Label>Reason</Label>
            <Select value={lostReason} onValueChange={(v) => setLostReason(v as LostReason)}>
              <SelectTrigger>
                <SelectValue placeholder="Why was this lost?" />
              </SelectTrigger>
              <SelectContent>
                {LOST_REASONS.map((r) => (
                  <SelectItem key={r} value={r}>
                    {LOST_REASON_LABELS[r]}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="lostComment">Comment (optional)</Label>
            <Textarea
              id="lostComment"
              rows={3}
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              placeholder="e.g. Customer received a lower quotation from another supplier"
            />
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" variant="destructive" disabled={!lostReason || markLost.isPending}>
              {markLost.isPending ? "Saving…" : "Mark Lost"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

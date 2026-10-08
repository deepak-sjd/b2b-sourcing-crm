import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Users, Plus, CheckCircle2, CalendarClock, XCircle, UserX } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/common/EmptyState";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogTrigger, DialogDescription } from "@/components/ui/dialog";
import { activitiesApi } from "@/api/activities";
import { extractErrorMessage } from "@/api/client";
import { formatDate } from "@/lib/utils";
import {
  MEETING_TYPES, MEETING_TYPE_LABELS, MEETING_STATUS_LABELS,
  type Activity, type MeetingType, type MeetingStatus,
} from "@/types/activity";

const MEETING_STATUS_CLASS: Record<MeetingStatus, string> = {
  SCHEDULED: "bg-accent text-accent-foreground",
  COMPLETED: "bg-emerald-100 text-emerald-700",
  RESCHEDULED: "bg-amber-100 text-amber-800",
  CANCELLED: "bg-muted text-muted-foreground",
  NO_SHOW: "bg-red-100 text-red-700",
};

function toDatetimeLocal(d: Date) {
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

/** Status-change dialogs (Complete/Reschedule/Cancel/No-show) share this
 *  shape - only the fields relevant to the action render. */
function MeetingActionDialog({
  meeting, action, open, onOpenChange,
}: {
  meeting: Activity; action: MeetingStatus; open: boolean; onOpenChange: (open: boolean) => void;
}) {
  const queryClient = useQueryClient();
  const [notes, setNotes] = useState("");
  const [newScheduledAt, setNewScheduledAt] = useState(toDatetimeLocal(new Date()));

  const mutation = useMutation({
    mutationFn: () =>
      activitiesApi.changeMeetingStatus(meeting.id, {
        status: action,
        notes: notes.trim() || null,
        newScheduledAt: action === "RESCHEDULED" ? new Date(newScheduledAt).toISOString() : null,
      }),
    onSuccess: () => {
      toast.success(`Meeting marked ${MEETING_STATUS_LABELS[action]}`);
      queryClient.invalidateQueries({ queryKey: ["activities"] });
      onOpenChange(false);
      setNotes("");
    },
    onError: (error) => toast.error(extractErrorMessage(error, "Couldn't update this meeting")),
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Mark meeting as {MEETING_STATUS_LABELS[action]}</DialogTitle>
          <DialogDescription>{meeting.lead.fullName}</DialogDescription>
        </DialogHeader>
        <form onSubmit={(e) => { e.preventDefault(); mutation.mutate(); }} className="space-y-3">
          {action === "RESCHEDULED" && (
            <div className="space-y-1.5">
              <Label>New date & time</Label>
              <Input type="datetime-local" value={newScheduledAt} onChange={(e) => setNewScheduledAt(e.target.value)} required />
            </div>
          )}
          {(action === "COMPLETED" || action === "CANCELLED" || action === "NO_SHOW") && (
            <div className="space-y-1.5">
              <Label>Notes (optional)</Label>
              <Textarea rows={3} value={notes} onChange={(e) => setNotes(e.target.value)} />
            </div>
          )}
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
            <Button type="submit" disabled={mutation.isPending}>{mutation.isPending ? "Saving…" : "Confirm"}</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

function ScheduleMeetingDialog({ leadId, open, onOpenChange }: { leadId: string; open: boolean; onOpenChange: (open: boolean) => void }) {
  const queryClient = useQueryClient();
  const [meetingType, setMeetingType] = useState<MeetingType>("VIDEO");
  const [scheduledAt, setScheduledAt] = useState(toDatetimeLocal(new Date(Date.now() + 86_400_000)));
  const [contactPerson, setContactPerson] = useState("");
  const [location, setLocation] = useState("");
  const [agenda, setAgenda] = useState("");

  const schedule = useMutation({
    mutationFn: () =>
      activitiesApi.log({
        leadId, activityType: "MEETING", status: "SCHEDULED",
        scheduledAt: new Date(scheduledAt).toISOString(),
        meetingType, agenda: agenda.trim() || null,
        contactPerson: contactPerson.trim() || null,
        location: location.trim() || null,
      }),
    onSuccess: () => {
      toast.success("Meeting scheduled");
      queryClient.invalidateQueries({ queryKey: ["activities"] });
      queryClient.invalidateQueries({ queryKey: ["lead", leadId] });
      onOpenChange(false);
      setContactPerson(""); setLocation(""); setAgenda("");
    },
    onError: (error) => toast.error(extractErrorMessage(error, "Couldn't schedule this meeting")),
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogTrigger asChild>
        <Button size="sm"><Plus className="h-4 w-4" />Schedule meeting</Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader><DialogTitle>Schedule a meeting</DialogTitle></DialogHeader>
        <form onSubmit={(e) => { e.preventDefault(); schedule.mutate(); }} className="space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <Label>Type</Label>
              <Select value={meetingType} onValueChange={(v) => setMeetingType(v as MeetingType)}>
                <SelectTrigger><SelectValue /></SelectTrigger>
                <SelectContent>
                  {MEETING_TYPES.map((t) => <SelectItem key={t} value={t}>{MEETING_TYPE_LABELS[t]}</SelectItem>)}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1.5">
              <Label>Date & time</Label>
              <Input type="datetime-local" value={scheduledAt} onChange={(e) => setScheduledAt(e.target.value)} required />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <Label htmlFor="meeting-contact">Contact person (optional)</Label>
              <Input id="meeting-contact" value={contactPerson} onChange={(e) => setContactPerson(e.target.value)} />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="meeting-location">Location / link (optional)</Label>
              <Input id="meeting-location" value={location} onChange={(e) => setLocation(e.target.value)} />
            </div>
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="meeting-agenda">Agenda</Label>
            <Textarea id="meeting-agenda" rows={3} value={agenda} onChange={(e) => setAgenda(e.target.value)} />
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
            <Button type="submit" disabled={schedule.isPending}>{schedule.isPending ? "Scheduling…" : "Schedule"}</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

export function LeadMeetingsTab({ leadId }: { leadId: string }) {
  const [scheduleOpen, setScheduleOpen] = useState(false);
  const [actionFor, setActionFor] = useState<{ meeting: Activity; action: MeetingStatus } | null>(null);

  const meetingsQuery = useQuery({
    queryKey: ["activities", "lead", leadId, "MEETING"],
    queryFn: () => activitiesApi.byLead(leadId, "MEETING", 0, 50),
  });

  const meetings = meetingsQuery.data?.content ?? [];

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-end">
        <ScheduleMeetingDialog leadId={leadId} open={scheduleOpen} onOpenChange={setScheduleOpen} />
      </div>

      {meetingsQuery.isLoading ? (
        <div className="space-y-2">
          <Skeleton className="h-20 w-full" /><Skeleton className="h-20 w-full" />
        </div>
      ) : meetings.length === 0 ? (
        <EmptyState icon={Users} title="No meetings yet" description="Scheduled and past meetings for this lead will show up here." />
      ) : (
        <div className="space-y-3">
          {meetings.map((m) => {
            const status = m.meetingStatus ?? "SCHEDULED";
            const canAct = status === "SCHEDULED" || status === "RESCHEDULED";
            return (
              <div key={m.id} className="rounded-lg border border-border p-3">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="text-sm font-medium">
                      {m.meetingType ? MEETING_TYPE_LABELS[m.meetingType] : "Meeting"}
                      {m.contactPerson ? ` · with ${m.contactPerson}` : ""}
                    </p>
                    {m.agenda && <p className="mt-0.5 text-sm text-foreground">{m.agenda}</p>}
                    {m.notes && <p className="text-sm text-muted-foreground">{m.notes}</p>}
                    <p className="mt-1 text-xs text-muted-foreground">
                      {formatDate(m.scheduledAt, true)}{m.location ? ` · ${m.location}` : ""}
                    </p>
                  </div>
                  <span className={`shrink-0 rounded-full px-2 py-0.5 text-xs font-medium ${MEETING_STATUS_CLASS[status]}`}>
                    {MEETING_STATUS_LABELS[status]}
                  </span>
                </div>
                {canAct && (
                  <div className="mt-2.5 flex flex-wrap gap-1.5">
                    <Button size="sm" variant="outline" onClick={() => setActionFor({ meeting: m, action: "COMPLETED" })}>
                      <CheckCircle2 className="h-3.5 w-3.5" />Complete
                    </Button>
                    <Button size="sm" variant="outline" onClick={() => setActionFor({ meeting: m, action: "RESCHEDULED" })}>
                      <CalendarClock className="h-3.5 w-3.5" />Reschedule
                    </Button>
                    <Button size="sm" variant="outline" onClick={() => setActionFor({ meeting: m, action: "NO_SHOW" })}>
                      <UserX className="h-3.5 w-3.5" />No show
                    </Button>
                    <Button size="sm" variant="outline" onClick={() => setActionFor({ meeting: m, action: "CANCELLED" })}>
                      <XCircle className="h-3.5 w-3.5" />Cancel
                    </Button>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      {actionFor && (
        <MeetingActionDialog
          meeting={actionFor.meeting}
          action={actionFor.action}
          open={!!actionFor}
          onOpenChange={(open) => !open && setActionFor(null)}
        />
      )}
    </div>
  );
}
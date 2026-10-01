import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Activity as ActivityIcon, Phone, Users, MapPin, Mail, MessageCircle, MoreHorizontal, Clock } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/common/EmptyState";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogTrigger } from "@/components/ui/dialog";
import { activitiesApi } from "@/api/activities";
import { extractErrorMessage } from "@/api/client";
import { formatDate } from "@/lib/utils";
import {
  ACTIVITY_TYPES, ACTIVITY_TYPE_LABELS, ACTIVITY_DIRECTIONS, ACTIVITY_STATUSES, ACTIVITY_SENTIMENTS,
  type ActivityType, type ActivityDirection, type ActivityStatus, type ActivitySentiment,
} from "@/types/activity";

const TYPE_ICON: Record<ActivityType, React.ComponentType<{ className?: string }>> = {
  CALL: Phone, MEETING: Users, SITE_VISIT: MapPin, EMAIL: Mail, WHATSAPP: MessageCircle, OTHER: MoreHorizontal,
};

const STATUS_CLASS: Record<ActivityStatus, string> = {
  SCHEDULED: "bg-accent text-accent-foreground",
  COMPLETED: "bg-emerald-100 text-emerald-700",
  MISSED: "bg-red-100 text-red-700",
  NO_ANSWER: "bg-amber-100 text-amber-800",
  BUSY: "bg-amber-100 text-amber-800",
};

// Quick-pick follow-up presets instead of forcing a raw datetime picker for
// the common cases — "Custom" still falls back to one.
const FOLLOW_UP_PRESETS: { label: string; hoursFromNow: number | null }[] = [
  { label: "None", hoursFromNow: null },
  { label: "In 2h", hoursFromNow: 2 },
  { label: "Tomorrow", hoursFromNow: 24 },
  { label: "In 3 days", hoursFromNow: 72 },
  { label: "In 1 week", hoursFromNow: 168 },
];

function toDatetimeLocal(d: Date) {
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export function LeadActivitiesTab({ leadId }: { leadId: string }) {
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [typeFilter, setTypeFilter] = useState<ActivityType | "ALL">("ALL");

  const [activityType, setActivityType] = useState<ActivityType>("CALL");
  const [direction, setDirection] = useState<ActivityDirection>("OUTBOUND");
  const [status, setStatus] = useState<ActivityStatus>("COMPLETED");
  const [contactPerson, setContactPerson] = useState("");
  const [location, setLocation] = useState("");
  const [durationSeconds, setDurationSeconds] = useState("");
  const [sentiment, setSentiment] = useState<ActivitySentiment | "">("");
  const [requirements, setRequirements] = useState("");
  const [notes, setNotes] = useState("");
  const [taggedResourceEmail, setTaggedResourceEmail] = useState("");
  const [followUpPreset, setFollowUpPreset] = useState("None");
  const [customFollowUp, setCustomFollowUp] = useState("");

  const needsDirection = activityType === "CALL" || activityType === "EMAIL" || activityType === "WHATSAPP";
  const needsLocation = activityType === "MEETING" || activityType === "SITE_VISIT";
  const needsDuration = activityType === "CALL" || activityType === "MEETING";

  const activitiesQuery = useQuery({
    queryKey: ["activities", "lead", leadId, typeFilter],
    queryFn: () => activitiesApi.byLead(leadId, typeFilter === "ALL" ? undefined : typeFilter, 0, 50),
  });

  function resetForm() {
    setContactPerson(""); setLocation(""); setDurationSeconds(""); setSentiment("");
    setRequirements(""); setNotes(""); setTaggedResourceEmail(""); setFollowUpPreset("None"); setCustomFollowUp("");
  }

  const logActivity = useMutation({
    mutationFn: () => {
      let followUpAt: string | null = null;
      if (followUpPreset === "Custom" && customFollowUp) {
        followUpAt = new Date(customFollowUp).toISOString();
      } else {
        const preset = FOLLOW_UP_PRESETS.find((p) => p.label === followUpPreset);
        if (preset?.hoursFromNow) followUpAt = new Date(Date.now() + preset.hoursFromNow * 3600_000).toISOString();
      }
      return activitiesApi.log({
        leadId,
        activityType,
        direction: needsDirection ? direction : null,
        status,
        contactPerson: contactPerson.trim() || null,
        location: needsLocation ? location.trim() || null : null,
        durationSeconds: needsDuration && durationSeconds ? Number(durationSeconds) : null,
        sentiment: sentiment || null,
        requirements: requirements.trim() || null,
        notes: notes.trim() || null,
        followUpAt,
        taggedResourceEmail: taggedResourceEmail.trim() || null,
      });
    },
    onSuccess: () => {
      toast.success("Activity logged");
      resetForm();
      setOpen(false);
      queryClient.invalidateQueries({ queryKey: ["activities"] });
      queryClient.invalidateQueries({ queryKey: ["lead", leadId] });
    },
    onError: (error) => toast.error(extractErrorMessage(error, "Couldn't log activity")),
  });

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex flex-wrap gap-1.5">
          <button
            onClick={() => setTypeFilter("ALL")}
            className={`rounded-full border px-3 py-1 text-xs font-medium transition-colors ${
              typeFilter === "ALL" ? "border-primary bg-primary text-primary-foreground" : "border-border text-muted-foreground hover:bg-muted"
            }`}
          >
            All
          </button>
          {ACTIVITY_TYPES.map((t) => (
            <button
              key={t}
              onClick={() => setTypeFilter(t)}
              className={`rounded-full border px-3 py-1 text-xs font-medium transition-colors ${
                typeFilter === t ? "border-primary bg-primary text-primary-foreground" : "border-border text-muted-foreground hover:bg-muted"
              }`}
            >
              {ACTIVITY_TYPE_LABELS[t]}
            </button>
          ))}
        </div>

        <Dialog open={open} onOpenChange={setOpen}>
          <DialogTrigger asChild>
            <Button size="sm">
              <ActivityIcon className="h-4 w-4" />
              Log activity
            </Button>
          </DialogTrigger>
          <DialogContent className="max-h-[85vh] overflow-y-auto">
            <DialogHeader>
              <DialogTitle>Log an activity</DialogTitle>
            </DialogHeader>
            <form onSubmit={(e) => { e.preventDefault(); logActivity.mutate(); }} className="space-y-3">
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1.5">
                  <Label>Type</Label>
                  <Select value={activityType} onValueChange={(v) => setActivityType(v as ActivityType)}>
                    <SelectTrigger><SelectValue /></SelectTrigger>
                    <SelectContent>
                      {ACTIVITY_TYPES.map((t) => <SelectItem key={t} value={t}>{ACTIVITY_TYPE_LABELS[t]}</SelectItem>)}
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-1.5">
                  <Label>Outcome</Label>
                  <Select value={status} onValueChange={(v) => setStatus(v as ActivityStatus)}>
                    <SelectTrigger><SelectValue /></SelectTrigger>
                    <SelectContent>
                      {ACTIVITY_STATUSES.map((s) => <SelectItem key={s} value={s}>{s.replaceAll("_", " ")}</SelectItem>)}
                    </SelectContent>
                  </Select>
                </div>
              </div>

              {needsDirection && (
                <div className="space-y-1.5">
                  <Label>Direction</Label>
                  <Select value={direction} onValueChange={(v) => setDirection(v as ActivityDirection)}>
                    <SelectTrigger><SelectValue /></SelectTrigger>
                    <SelectContent>
                      {ACTIVITY_DIRECTIONS.map((d) => <SelectItem key={d} value={d}>{d[0] + d.slice(1).toLowerCase()}</SelectItem>)}
                    </SelectContent>
                  </Select>
                </div>
              )}

              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1.5">
                  <Label htmlFor="contact-person">Spoke with (optional)</Label>
                  <Input id="contact-person" placeholder="Same as lead" value={contactPerson} onChange={(e) => setContactPerson(e.target.value)} />
                </div>
                {needsLocation && (
                  <div className="space-y-1.5">
                    <Label htmlFor="location">Location</Label>
                    <Input id="location" value={location} onChange={(e) => setLocation(e.target.value)} />
                  </div>
                )}
                {needsDuration && (
                  <div className="space-y-1.5">
                    <Label htmlFor="duration">Duration (seconds)</Label>
                    <Input id="duration" type="number" min="0" value={durationSeconds} onChange={(e) => setDurationSeconds(e.target.value)} />
                  </div>
                )}
                <div className="space-y-1.5">
                  <Label>Sentiment (optional)</Label>
                  <Select value={sentiment} onValueChange={(v) => setSentiment(v as ActivitySentiment)}>
                    <SelectTrigger><SelectValue placeholder="—" /></SelectTrigger>
                    <SelectContent>
                      {ACTIVITY_SENTIMENTS.map((s) => <SelectItem key={s} value={s}>{s[0] + s.slice(1).toLowerCase()}</SelectItem>)}
                    </SelectContent>
                  </Select>
                </div>
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="requirements">Customer requirements (optional)</Label>
                <Textarea id="requirements" rows={2} value={requirements} onChange={(e) => setRequirements(e.target.value)} />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="activity-notes">Notes</Label>
                <Textarea id="activity-notes" rows={2} value={notes} onChange={(e) => setNotes(e.target.value)} />
              </div>

              <div className="space-y-1.5">
                <Label>Follow up</Label>
                <div className="flex flex-wrap gap-1.5">
                  {FOLLOW_UP_PRESETS.map((p) => (
                    <button
                      type="button"
                      key={p.label}
                      onClick={() => setFollowUpPreset(p.label)}
                      className={`rounded-full border px-2.5 py-1 text-xs font-medium ${
                        followUpPreset === p.label ? "border-primary bg-primary text-primary-foreground" : "border-border text-muted-foreground hover:bg-muted"
                      }`}
                    >
                      {p.label}
                    </button>
                  ))}
                  <button
                    type="button"
                    onClick={() => { setFollowUpPreset("Custom"); setCustomFollowUp(toDatetimeLocal(new Date())); }}
                    className={`rounded-full border px-2.5 py-1 text-xs font-medium ${
                      followUpPreset === "Custom" ? "border-primary bg-primary text-primary-foreground" : "border-border text-muted-foreground hover:bg-muted"
                    }`}
                  >
                    Custom
                  </button>
                </div>
                {followUpPreset === "Custom" && (
                  <Input type="datetime-local" className="mt-1.5" value={customFollowUp} onChange={(e) => setCustomFollowUp(e.target.value)} />
                )}
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="tag-resource">Tag a colleague for help (email, optional)</Label>
                <Input id="tag-resource" type="email" value={taggedResourceEmail} onChange={(e) => setTaggedResourceEmail(e.target.value)} />
              </div>

              <DialogFooter>
                <Button type="button" variant="outline" onClick={() => setOpen(false)}>Cancel</Button>
                <Button type="submit" disabled={logActivity.isPending}>{logActivity.isPending ? "Saving…" : "Log activity"}</Button>
              </DialogFooter>
            </form>
          </DialogContent>
        </Dialog>
      </div>

      {activitiesQuery.isLoading ? (
        <div className="space-y-2">
          <Skeleton className="h-16 w-full" /><Skeleton className="h-16 w-full" />
        </div>
      ) : activitiesQuery.data && activitiesQuery.data.content.length > 0 ? (
        <ol className="relative space-y-4 border-l border-border pl-6">
          {activitiesQuery.data.content.map((a) => {
            const Icon = TYPE_ICON[a.activityType];
            const overdue = a.followUpAt && new Date(a.followUpAt).getTime() < Date.now();
            return (
              <li key={a.id} className="relative">
                <span className="absolute -left-[29px] flex h-6 w-6 items-center justify-center rounded-full border border-border bg-background">
                  <Icon className="h-3.5 w-3.5 text-muted-foreground" />
                </span>
                <div className="flex items-start justify-between gap-3 rounded-lg border border-border p-3">
                  <div>
                    <p className="text-sm font-medium">
                      {ACTIVITY_TYPE_LABELS[a.activityType]}
                      {a.contactPerson ? ` · with ${a.contactPerson}` : ""}
                      {a.loggedBy ? ` · by ${a.loggedBy.fullName}` : ""}
                    </p>
                    {a.requirements && <p className="mt-0.5 text-sm text-foreground">{a.requirements}</p>}
                    {a.notes && <p className="text-sm text-muted-foreground">{a.notes}</p>}
                    <p className="mt-1 flex flex-wrap items-center gap-x-2 text-xs text-muted-foreground">
                      <span>{formatDate(a.scheduledAt, true)}</span>
                      {a.durationSeconds ? <span>· {a.durationSeconds}s</span> : null}
                      {a.location ? <span>· {a.location}</span> : null}
                      {a.taggedResource ? <span>· help: {a.taggedResource.fullName}</span> : null}
                    </p>
                    {a.followUpAt && (
                      <p className={`mt-1 inline-flex items-center gap-1 text-xs font-medium ${overdue ? "text-red-600" : "text-muted-foreground"}`}>
                        <Clock className="h-3 w-3" />
                        Follow up {formatDate(a.followUpAt, true)}
                      </p>
                    )}
                  </div>
                  <span className={`shrink-0 rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_CLASS[a.status]}`}>
                    {a.status.replaceAll("_", " ")}
                  </span>
                </div>
              </li>
            );
          })}
        </ol>
      ) : (
        <EmptyState icon={ActivityIcon} title="No activity yet" description="Calls, meetings, and other touchpoints for this lead will show up here." />
      )}
    </div>
  );
}
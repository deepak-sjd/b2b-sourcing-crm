import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { StickyNote } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/common/EmptyState";
import { notesApi } from "@/api/notes";
import { extractErrorMessage } from "@/api/client";
import { formatDate, initials } from "@/lib/utils";
import { useAuthStore } from "@/store/authStore";

export function LeadNotesTab({ leadId }: { leadId: string }) {
  const queryClient = useQueryClient();
  const currentUser = useAuthStore((s) => s.user);
  const [content, setContent] = useState("");

  const notesQuery = useQuery({
    queryKey: ["notes", leadId],
    queryFn: () => notesApi.byLead(leadId),
  });

  const addNote = useMutation({
    mutationFn: () => notesApi.add({ leadId, content: content.trim(), authorId: currentUser?.id }),
    onSuccess: () => {
      setContent("");
      queryClient.invalidateQueries({ queryKey: ["notes", leadId] });
    },
    onError: (error) => toast.error(extractErrorMessage(error, "Couldn't add note")),
  });

  return (
    <div className="space-y-4">
      <form
        onSubmit={(e) => {
          e.preventDefault();
          if (content.trim()) addNote.mutate();
        }}
        className="space-y-2"
      >
        <Textarea
          placeholder="Add a note about this lead…"
          value={content}
          onChange={(e) => setContent(e.target.value)}
          rows={3}
        />
        <div className="flex justify-end">
          <Button type="submit" size="sm" disabled={!content.trim() || addNote.isPending}>
            {addNote.isPending ? "Saving…" : "Add note"}
          </Button>
        </div>
      </form>

      {notesQuery.isLoading ? (
        <div className="space-y-2">
          <Skeleton className="h-16 w-full" />
          <Skeleton className="h-16 w-full" />
        </div>
      ) : notesQuery.data && notesQuery.data.content.length > 0 ? (
        <div className="space-y-3">
          {notesQuery.data.content.map((note) => (
            <div key={note.id} className="flex gap-3 rounded-lg border border-border p-3">
              <Avatar className="h-8 w-8">
                <AvatarFallback>{initials(note.author?.fullName)}</AvatarFallback>
              </Avatar>
              <div className="min-w-0 flex-1">
                <div className="flex items-baseline justify-between gap-2">
                  <p className="text-sm font-medium">{note.author?.fullName ?? "Unknown"}</p>
                  <p className="shrink-0 text-xs text-muted-foreground">{formatDate(note.createdAt, true)}</p>
                </div>
                <p className="mt-0.5 whitespace-pre-wrap text-sm text-foreground">{note.content}</p>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <EmptyState icon={StickyNote} title="No notes yet" description="Notes from calls and check-ins will show up here." />
      )}
    </div>
  );
}

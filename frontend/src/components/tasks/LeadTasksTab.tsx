import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { CheckSquare, Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/common/EmptyState";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  DialogTrigger,
} from "@/components/ui/dialog";
import { tasksApi } from "@/api/tasks";
import { extractErrorMessage } from "@/api/client";
import { formatDate } from "@/lib/utils";
import { TASK_STATUS_LABELS, type TaskStatus } from "@/types/task";

const STATUS_CLASS: Record<TaskStatus, string> = {
  PENDING: "bg-muted text-muted-foreground",
  IN_PROGRESS: "bg-sky-100 text-sky-700",
  COMPLETED: "bg-emerald-100 text-emerald-700",
  OVERDUE: "bg-red-100 text-red-700",
  CANCELLED: "bg-muted text-muted-foreground line-through",
};

export function LeadTasksTab({ leadId }: { leadId: string }) {
  const queryClient = useQueryClient();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [dueDate, setDueDate] = useState("");

  const tasksQuery = useQuery({ queryKey: ["tasks", "lead", leadId], queryFn: () => tasksApi.byLead(leadId) });

  const createTask = useMutation({
    mutationFn: () =>
      tasksApi.create({
        title: title.trim(),
        description: description.trim() || null,
        leadId,
        dueDate: dueDate ? new Date(dueDate).toISOString() : null,
      }),
    onSuccess: () => {
      toast.success("Task created");
      setTitle("");
      setDescription("");
      setDueDate("");
      setDialogOpen(false);
      queryClient.invalidateQueries({ queryKey: ["tasks"] });
    },
    onError: (error) => toast.error(extractErrorMessage(error, "Couldn't create task")),
  });

  const completeTask = useMutation({
    mutationFn: (taskId: string) => tasksApi.complete(taskId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["tasks"] }),
    onError: (error) => toast.error(extractErrorMessage(error, "Couldn't complete task")),
  });

  return (
    <div className="space-y-4">
      <div className="flex justify-end">
        <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
          <DialogTrigger asChild>
            <Button size="sm">
              <Plus className="h-4 w-4" />
              New task
            </Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>New task</DialogTitle>
            </DialogHeader>
            <form
              onSubmit={(e) => {
                e.preventDefault();
                if (title.trim()) createTask.mutate();
              }}
              className="space-y-3"
            >
              <div className="space-y-1.5">
                <Label htmlFor="task-title">Title</Label>
                <Input id="task-title" value={title} onChange={(e) => setTitle(e.target.value)} autoFocus />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="task-desc">Description</Label>
                <Textarea id="task-desc" value={description} onChange={(e) => setDescription(e.target.value)} rows={3} />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="task-due">Due date</Label>
                <Input id="task-due" type="datetime-local" value={dueDate} onChange={(e) => setDueDate(e.target.value)} />
              </div>
              <DialogFooter>
                <Button type="button" variant="outline" onClick={() => setDialogOpen(false)}>
                  Cancel
                </Button>
                <Button type="submit" disabled={!title.trim() || createTask.isPending}>
                  {createTask.isPending ? "Creating…" : "Create task"}
                </Button>
              </DialogFooter>
            </form>
          </DialogContent>
        </Dialog>
      </div>

      {tasksQuery.isLoading ? (
        <div className="space-y-2">
          <Skeleton className="h-14 w-full" />
          <Skeleton className="h-14 w-full" />
        </div>
      ) : tasksQuery.data && tasksQuery.data.content.length > 0 ? (
        <div className="divide-y divide-border rounded-lg border border-border">
          {tasksQuery.data.content.map((task) => (
            <div key={task.id} className="flex items-start justify-between gap-3 p-3">
              <div className="min-w-0">
                <p className="text-sm font-medium">{task.title}</p>
                {task.description && <p className="text-sm text-muted-foreground">{task.description}</p>}
                <p className="mt-1 text-xs text-muted-foreground">
                  Due {formatDate(task.dueDate, true)}
                  {task.assignedTo ? ` · ${task.assignedTo.fullName}` : ""}
                </p>
              </div>
              <div className="flex shrink-0 items-center gap-2">
                <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_CLASS[task.status]}`}>
                  {TASK_STATUS_LABELS[task.status]}
                </span>
                {task.status !== "COMPLETED" && task.status !== "CANCELLED" && (
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => completeTask.mutate(task.id)}
                    disabled={completeTask.isPending}
                  >
                    Mark done
                  </Button>
                )}
              </div>
            </div>
          ))}
        </div>
      ) : (
        <EmptyState icon={CheckSquare} title="No tasks yet" description="Follow-ups and reminders for this lead will show up here." />
      )}
    </div>
  );
}

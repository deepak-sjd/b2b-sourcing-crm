import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { CheckSquare } from "lucide-react";
import { PageHeader } from "@/components/common/PageHeader";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/common/EmptyState";
import { Pagination } from "@/components/common/Pagination";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from "@/components/ui/table";
import { tasksApi } from "@/api/tasks";
import { extractErrorMessage } from "@/api/client";
import { formatDate } from "@/lib/utils";
import { TASK_STATUSES, TASK_STATUS_LABELS, type TaskStatus } from "@/types/task";

const PAGE_SIZE = 20;

const STATUS_CLASS: Record<TaskStatus, string> = {
  PENDING: "bg-muted text-muted-foreground",
  IN_PROGRESS: "bg-sky-100 text-sky-700",
  COMPLETED: "bg-emerald-100 text-emerald-700",
  OVERDUE: "bg-red-100 text-red-700",
  CANCELLED: "bg-muted text-muted-foreground line-through",
};

function SummaryCard({ label, value, loading }: { label: string; value: React.ReactNode; loading: boolean }) {
  return (
    <Card>
      <CardContent className="p-5">
        <p className="text-sm text-muted-foreground">{label}</p>
        {loading ? <Skeleton className="mt-2 h-7 w-12" /> : <p className="mt-1 text-2xl font-semibold">{value}</p>}
      </CardContent>
    </Card>
  );
}

export function TasksPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState<TaskStatus | "ALL">("ALL");

  const summaryQuery = useQuery({ queryKey: ["tasks", "summary"], queryFn: tasksApi.summary });
  const tasksQuery = useQuery({
    queryKey: ["tasks", "list", { page, status }],
    queryFn: () => tasksApi.list({ page, size: PAGE_SIZE, status: status === "ALL" ? undefined : status }),
  });

  const completeTask = useMutation({
    mutationFn: (taskId: string) => tasksApi.complete(taskId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["tasks"] });
    },
    onError: (error) => toast.error(extractErrorMessage(error, "Couldn't complete task")),
  });

  return (
    <div>
      <PageHeader title="Tasks" description="Follow-ups and reminders across your pipeline." />

      <div className="mb-4 grid grid-cols-1 gap-4 sm:grid-cols-3">
        <SummaryCard label="Pending" value={summaryQuery.data?.pending ?? 0} loading={summaryQuery.isLoading} />
        <SummaryCard label="Completed" value={summaryQuery.data?.completed ?? 0} loading={summaryQuery.isLoading} />
        <SummaryCard label="Overdue" value={summaryQuery.data?.overdue ?? 0} loading={summaryQuery.isLoading} />
      </div>

      <Card>
        <div className="flex items-center gap-2 border-b border-border p-3">
          <Select
            value={status}
            onValueChange={(v) => {
              setStatus(v as TaskStatus | "ALL");
              setPage(0);
            }}
          >
            <SelectTrigger className="w-44">
              <SelectValue placeholder="Status" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">All statuses</SelectItem>
              {TASK_STATUSES.map((s) => (
                <SelectItem key={s} value={s}>
                  {TASK_STATUS_LABELS[s]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        {tasksQuery.isLoading ? (
          <div className="space-y-2 p-4">
            {Array.from({ length: 5 }).map((_, i) => (
              <Skeleton key={i} className="h-10 w-full" />
            ))}
          </div>
        ) : tasksQuery.data && tasksQuery.data.content.length > 0 ? (
          <>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Title</TableHead>
                  <TableHead>Lead</TableHead>
                  <TableHead>Assigned To</TableHead>
                  <TableHead>Due</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead />
                </TableRow>
              </TableHeader>
              <TableBody>
                {tasksQuery.data.content.map((task) => (
                  <TableRow key={task.id}>
                    <TableCell>
                      <p className="font-medium">{task.title}</p>
                      {task.description && (
                        <p className="max-w-xs truncate text-xs text-muted-foreground">{task.description}</p>
                      )}
                    </TableCell>
                    <TableCell className="text-muted-foreground">{task.lead?.fullName ?? "—"}</TableCell>
                    <TableCell className="text-muted-foreground">{task.assignedTo?.fullName ?? "Unassigned"}</TableCell>
                    <TableCell className="text-muted-foreground">{formatDate(task.dueDate, true)}</TableCell>
                    <TableCell>
                      <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_CLASS[task.status]}`}>
                        {TASK_STATUS_LABELS[task.status]}
                      </span>
                    </TableCell>
                    <TableCell>
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
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            <Pagination
              page={tasksQuery.data.page}
              totalPages={tasksQuery.data.totalPages}
              totalElements={tasksQuery.data.totalElements}
              onPageChange={setPage}
            />
          </>
        ) : (
          <EmptyState icon={CheckSquare} title="No tasks to show" description="Try a different status filter, or create a task from a lead's Tasks tab." />
        )}
      </Card>
    </div>
  );
}

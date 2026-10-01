import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Plus, Users } from "lucide-react";
import { PageHeader } from "@/components/common/PageHeader";
import { Card, CardContent } from "@/components/ui/card";
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
import { teamsApi } from "@/api/teams";
import { extractErrorMessage } from "@/api/client";

export function TeamsPage() {
  const queryClient = useQueryClient();
  const teamsQuery = useQuery({ queryKey: ["teams"], queryFn: teamsApi.listAll });

  const [open, setOpen] = useState(false);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");

  const createTeam = useMutation({
    mutationFn: () => teamsApi.create({ name: name.trim(), description: description.trim() || null }),
    onSuccess: () => {
      toast.success("Team created");
      setName("");
      setDescription("");
      setOpen(false);
      queryClient.invalidateQueries({ queryKey: ["teams"] });
    },
    onError: (error) => toast.error(extractErrorMessage(error, "Couldn't create team")),
  });

  return (
    <div>
      <PageHeader
        title="Teams"
        description="Group counselors for reporting and task assignment."
        actions={
          <Dialog open={open} onOpenChange={setOpen}>
            <DialogTrigger asChild>
              <Button>
                <Plus className="h-4 w-4" />
                New team
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>New team</DialogTitle>
              </DialogHeader>
              <form
                onSubmit={(e) => {
                  e.preventDefault();
                  if (name.trim()) createTeam.mutate();
                }}
                className="space-y-3"
              >
                <div className="space-y-1.5">
                  <Label htmlFor="t-name">Name</Label>
                  <Input id="t-name" value={name} onChange={(e) => setName(e.target.value)} />
                </div>
                <div className="space-y-1.5">
                  <Label htmlFor="t-desc">Description</Label>
                  <Textarea id="t-desc" value={description} onChange={(e) => setDescription(e.target.value)} rows={3} />
                </div>
                <DialogFooter>
                  <Button type="button" variant="outline" onClick={() => setOpen(false)}>
                    Cancel
                  </Button>
                  <Button type="submit" disabled={createTeam.isPending}>
                    {createTeam.isPending ? "Creating…" : "Create team"}
                  </Button>
                </DialogFooter>
              </form>
            </DialogContent>
          </Dialog>
        }
      />

      {teamsQuery.isLoading ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <Skeleton className="h-28 w-full" />
          <Skeleton className="h-28 w-full" />
        </div>
      ) : teamsQuery.data && teamsQuery.data.length > 0 ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {teamsQuery.data.map((team) => (
            <Card key={team.id}>
              <CardContent className="p-5">
                <div className="flex items-center gap-2">
                  <div className="flex h-8 w-8 items-center justify-center rounded-md bg-accent">
                    <Users className="h-4 w-4 text-accent-foreground" />
                  </div>
                  <p className="font-medium">{team.name}</p>
                </div>
                <p className="mt-2 text-sm text-muted-foreground">{team.description ?? "No description"}</p>
              </CardContent>
            </Card>
          ))}
        </div>
      ) : (
        <Card>
          <EmptyState icon={Users} title="No teams yet" description="Create a team to start grouping counselors." />
        </Card>
      )}
    </div>
  );
}

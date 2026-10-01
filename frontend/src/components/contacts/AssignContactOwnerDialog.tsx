import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { contactsApi } from "@/api/contacts";
import { usersApi } from "@/api/users";
import { extractErrorMessage } from "@/api/client";
import { useAuthStore } from "@/store/authStore";
import type { ContactResponse } from "@/types/contact";

interface AssignContactOwnerDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  contact: ContactResponse;
  onAssigned: (contact: ContactResponse) => void;
}

export function AssignContactOwnerDialog({ open, onOpenChange, contact, onAssigned }: AssignContactOwnerDialogProps) {
  const role = useAuthStore((s) => s.user?.role);
  const canBrowseUsers = role === "SUPER_ADMIN" || role === "ADMIN";
  const [ownerId, setOwnerId] = useState("");
  const queryClient = useQueryClient();

  // Same server-side restriction as leads: GET /api/v1/users is admin-only
  // (see UserController), so managers/counselors fall back to an ID field.
  const usersQuery = useQuery({
    queryKey: ["users", "all"],
    queryFn: usersApi.listAll,
    enabled: open && canBrowseUsers,
  });

  const assign = useMutation({
    mutationFn: () => contactsApi.assign(contact.id, ownerId),
    onSuccess: (updated) => {
      toast.success(`Assigned to ${updated.ownerName ?? "new owner"}`);
      queryClient.invalidateQueries({ queryKey: ["contacts"] });
      onAssigned(updated);
      onOpenChange(false);
      setOwnerId("");
    },
    onError: (error) => toast.error(extractErrorMessage(error, "Couldn't assign this contact")),
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Assign owner</DialogTitle>
          <DialogDescription>Currently owned by {contact.ownerName ?? "no one"}.</DialogDescription>
        </DialogHeader>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            if (ownerId) assign.mutate();
          }}
          className="space-y-3"
        >
          {canBrowseUsers ? (
            <div className="space-y-1.5">
              <Label>Owner</Label>
              <Select value={ownerId} onValueChange={setOwnerId}>
                <SelectTrigger>
                  <SelectValue placeholder={usersQuery.isLoading ? "Loading users…" : "Select a user"} />
                </SelectTrigger>
                <SelectContent>
                  {usersQuery.data?.map((u) => (
                    <SelectItem key={u.id} value={u.id}>
                      {u.fullName} ({u.role})
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          ) : (
            <div className="space-y-1.5">
              <Label htmlFor="ownerId">Owner user ID</Label>
              <Input
                id="ownerId"
                placeholder="Paste the user's ID"
                value={ownerId}
                onChange={(e) => setOwnerId(e.target.value)}
              />
              <p className="text-xs text-muted-foreground">
                The user directory is admin-only today, so ask an admin for the ID, or have them assign it
                directly from the Users page.
              </p>
            </div>
          )}
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={!ownerId || assign.isPending}>
              {assign.isPending ? "Assigning…" : "Assign"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

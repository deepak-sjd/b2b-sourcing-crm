import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import {
  Plus,
  Search,
  Contact2,
  Phone,
  MessageCircle,
  Mail,
  MoreHorizontal,
  Pencil,
  UserCog,
  History,
  Archive,
  Flame,
  Snowflake,
  AlertTriangle,
  Users2,
} from "lucide-react";
import { PageHeader } from "@/components/common/PageHeader";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/common/EmptyState";
import { Pagination } from "@/components/common/Pagination";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from "@/components/ui/table";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { EngagementBadge, EngagementScore } from "@/components/contacts/ContactBadges";
import { ContactFormDialog } from "@/components/contacts/ContactFormDialog";
import { AssignContactOwnerDialog } from "@/components/contacts/AssignContactOwnerDialog";
import { contactsApi } from "@/api/contacts";
import { extractErrorMessage } from "@/api/client";
import { useAuthStore } from "@/store/authStore";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import { cn, formatRelativeDay, initials } from "@/lib/utils";
import { CONTACT_ENGAGEMENTS, CONTACT_ENGAGEMENT_LABELS, type ContactEngagement, type ContactResponse } from "@/types/contact";

const PAGE_SIZE = 20;

function onlyDigits(value: string) {
  return value.replace(/[^\d+]/g, "");
}

export function ContactsPage() {
  const role = useAuthStore((s) => s.user?.role);
  const canArchive = role === "SUPER_ADMIN" || role === "ADMIN";
  const queryClient = useQueryClient();

  const [page, setPage] = useState(0);
  const [engagement, setEngagement] = useState<ContactEngagement | "ALL">("ALL");
  const [search, setSearch] = useState("");
  const debouncedSearch = useDebouncedValue(search, 300);

  const [formOpen, setFormOpen] = useState(false);
  const [editingContact, setEditingContact] = useState<ContactResponse | undefined>(undefined);
  const [assigningContact, setAssigningContact] = useState<ContactResponse | undefined>(undefined);

  const statsQuery = useQuery({
    queryKey: ["contacts", "stats"],
    queryFn: contactsApi.stats,
  });

  const contactsQuery = useQuery({
    queryKey: ["contacts", { page, engagement, search: debouncedSearch }],
    queryFn: () =>
      contactsApi.list({
        page,
        size: PAGE_SIZE,
        engagement: engagement === "ALL" ? undefined : engagement,
        search: debouncedSearch || undefined,
      }),
  });

  const touchpoint = useMutation({
    mutationFn: (id: string) => contactsApi.recordTouchpoint(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["contacts"] });
    },
  });

  const archive = useMutation({
    mutationFn: (id: string) => contactsApi.archive(id),
    onSuccess: () => {
      toast.success("Contact archived");
      queryClient.invalidateQueries({ queryKey: ["contacts"] });
    },
    onError: (error) => toast.error(extractErrorMessage(error, "Couldn't archive this contact")),
  });

  function openCreate() {
    setEditingContact(undefined);
    setFormOpen(true);
  }

  function openEdit(contact: ContactResponse) {
    setEditingContact(contact);
    setFormOpen(true);
  }

  /** Click-to-call / click-to-WhatsApp / click-to-email: opens the right app
   * AND silently logs the touchpoint, so "Last activity" and the engagement
   * score update themselves — no separate "log this call" step required. */
  function quickAction(contact: ContactResponse, kind: "call" | "whatsapp" | "email") {
    if (kind === "call" && contact.phone) {
      window.open(`tel:${onlyDigits(contact.phone)}`, "_self");
    } else if (kind === "whatsapp" && contact.phone) {
      window.open(`https://wa.me/${onlyDigits(contact.phone).replace(/^\+/, "")}`, "_blank");
    } else if (kind === "email" && contact.email) {
      window.open(`mailto:${contact.email}`, "_self");
    }
    touchpoint.mutate(contact.id);
  }

  const stats = statsQuery.data;

  return (
    <div>
      <PageHeader
        title="Contacts"
        description="Everyone you're in touch with at your customer accounts."
        actions={
          <Button onClick={openCreate}>
            <Plus className="h-4 w-4" />
            Add contact
          </Button>
        }
      />

      <div className="mb-4 grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
        <StatCard label="Total contacts" value={stats?.total} icon={Users2} loading={statsQuery.isLoading} />
        <StatCard
          label="Active"
          value={stats?.active}
          icon={Flame}
          tone="text-emerald-600"
          loading={statsQuery.isLoading}
        />
        <StatCard
          label="Cooling"
          value={stats?.cooling}
          icon={Snowflake}
          tone="text-amber-600"
          loading={statsQuery.isLoading}
        />
        <StatCard
          label="At risk"
          value={stats?.atRisk}
          icon={AlertTriangle}
          tone="text-red-600"
          loading={statsQuery.isLoading}
        />
        <StatCard
          label="Unassigned"
          value={stats?.unassigned}
          icon={UserCog}
          tone="text-muted-foreground"
          loading={statsQuery.isLoading}
        />
      </div>

      <Card>
        <div className="flex flex-wrap items-center gap-2 border-b border-border p-3">
          <div className="relative w-full max-w-xs">
            <Search className="pointer-events-none absolute left-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Search name, company, phone, email…"
              className="pl-8"
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setPage(0);
              }}
            />
          </div>
          <Select
            value={engagement}
            onValueChange={(v) => {
              setEngagement(v as ContactEngagement | "ALL");
              setPage(0);
            }}
          >
            <SelectTrigger className="w-40">
              <SelectValue placeholder="Engagement" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">All engagement</SelectItem>
              {CONTACT_ENGAGEMENTS.map((e) => (
                <SelectItem key={e} value={e}>
                  {CONTACT_ENGAGEMENT_LABELS[e]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        {contactsQuery.isLoading ? (
          <div className="space-y-2 p-4">
            {Array.from({ length: 6 }).map((_, i) => (
              <Skeleton key={i} className="h-12 w-full" />
            ))}
          </div>
        ) : (contactsQuery.data?.content.length ?? 0) === 0 ? (
          <EmptyState
            icon={Contact2}
            title="No contacts to show"
            description="Try a different search or filter, or add your first contact."
            action={
              <Button size="sm" className="mt-1" onClick={openCreate}>
                <Plus className="h-4 w-4" />
                Add contact
              </Button>
            }
          />
        ) : (
          <>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Contact</TableHead>
                  <TableHead>Company</TableHead>
                  <TableHead>Reach</TableHead>
                  <TableHead>Engagement</TableHead>
                  <TableHead>Last activity</TableHead>
                  <TableHead>Owner</TableHead>
                  <TableHead className="w-10" />
                </TableRow>
              </TableHeader>
              <TableBody>
                {contactsQuery.data?.content.map((contact) => (
                  <TableRow key={contact.id}>
                    <TableCell>
                      <div className="flex items-center gap-2.5">
                        <Avatar className="h-8 w-8">
                          <AvatarFallback>{initials(contact.fullName)}</AvatarFallback>
                        </Avatar>
                        <div>
                          <p className="font-medium">{contact.fullName}</p>
                          <p className="text-xs text-muted-foreground">{contact.designation ?? "—"}</p>
                        </div>
                      </div>
                    </TableCell>
                    <TableCell className="text-muted-foreground">{contact.company ?? "—"}</TableCell>
                    <TableCell>
                      <div className="flex items-center gap-1">
                        <ReachButton
                          icon={Phone}
                          disabled={!contact.phone}
                          label={contact.phone ? `Call ${contact.phone}` : "No phone on file"}
                          onClick={() => quickAction(contact, "call")}
                        />
                        <ReachButton
                          icon={MessageCircle}
                          disabled={!contact.phone}
                          label={contact.phone ? "Message on WhatsApp" : "No phone on file"}
                          onClick={() => quickAction(contact, "whatsapp")}
                        />
                        <ReachButton
                          icon={Mail}
                          disabled={!contact.email}
                          label={contact.email ? `Email ${contact.email}` : "No email on file"}
                          tone={contact.emailBounced ? "text-destructive" : undefined}
                          onClick={() => quickAction(contact, "email")}
                        />
                        {contact.emailBounced && (
                          <span className="ml-0.5 text-[10px] font-medium uppercase tracking-wide text-destructive">
                            Bounced
                          </span>
                        )}
                      </div>
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <EngagementBadge engagement={contact.engagement} />
                        <EngagementScore score={contact.engagementScore} />
                      </div>
                      {contact.aiInsight && (
                        <p className="mt-1 max-w-[220px] truncate text-xs text-muted-foreground" title={contact.aiInsight}>
                          {contact.aiInsight}
                        </p>
                      )}
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {formatRelativeDay(contact.lastActivityAt)}
                    </TableCell>
                    <TableCell className="text-muted-foreground">{contact.ownerName ?? "Unassigned"}</TableCell>
                    <TableCell>
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button variant="ghost" size="icon" className="h-8 w-8">
                            <MoreHorizontal className="h-4 w-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <DropdownMenuItem onClick={() => openEdit(contact)}>
                            <Pencil className="h-4 w-4" />
                            Edit
                          </DropdownMenuItem>
                          <DropdownMenuItem onClick={() => setAssigningContact(contact)}>
                            <UserCog className="h-4 w-4" />
                            Assign owner
                          </DropdownMenuItem>
                          <DropdownMenuItem onClick={() => touchpoint.mutate(contact.id)}>
                            <History className="h-4 w-4" />
                            Log activity now
                          </DropdownMenuItem>
                          {canArchive && (
                            <>
                              <DropdownMenuSeparator />
                              <DropdownMenuItem
                                className="text-destructive focus:text-destructive"
                                onClick={() => archive.mutate(contact.id)}
                              >
                                <Archive className="h-4 w-4" />
                                Archive
                              </DropdownMenuItem>
                            </>
                          )}
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            {contactsQuery.data && (
              <Pagination
                page={contactsQuery.data.page}
                totalPages={contactsQuery.data.totalPages}
                totalElements={contactsQuery.data.totalElements}
                onPageChange={setPage}
              />
            )}
          </>
        )}
      </Card>

      <ContactFormDialog
        open={formOpen}
        onOpenChange={setFormOpen}
        contact={editingContact}
        onSaved={() => {
          queryClient.invalidateQueries({ queryKey: ["contacts"] });
        }}
      />

      {assigningContact && (
        <AssignContactOwnerDialog
          open={!!assigningContact}
          onOpenChange={(open) => !open && setAssigningContact(undefined)}
          contact={assigningContact}
          onAssigned={() => setAssigningContact(undefined)}
        />
      )}
    </div>
  );
}

function ReachButton({
  icon: Icon,
  onClick,
  disabled,
  label,
  tone,
}: {
  icon: typeof Phone;
  onClick: () => void;
  disabled?: boolean;
  label: string;
  tone?: string;
}) {
  return (
    <button
      type="button"
      title={label}
      aria-label={label}
      disabled={disabled}
      onClick={onClick}
      className={cn(
        "flex h-7 w-7 items-center justify-center rounded-md text-muted-foreground transition-colors hover:bg-accent hover:text-accent-foreground disabled:pointer-events-none disabled:opacity-30",
        tone,
      )}
    >
      <Icon className="h-3.5 w-3.5" />
    </button>
  );
}

function StatCard({
  label,
  value,
  icon: Icon,
  tone,
  loading,
}: {
  label: string;
  value: number | undefined;
  icon: typeof Users2;
  tone?: string;
  loading?: boolean;
}) {
  return (
    <Card>
      <CardContent className="flex items-center justify-between gap-2 p-4">
        <div>
          <p className="text-xs font-medium text-muted-foreground">{label}</p>
          {loading ? (
            <Skeleton className="mt-1.5 h-6 w-8" />
          ) : (
            <p className="mt-0.5 text-xl font-semibold tracking-tight">{value ?? 0}</p>
          )}
        </div>
        <Icon className={cn("h-5 w-5", tone ?? "text-muted-foreground")} />
      </CardContent>
    </Card>
  );
}

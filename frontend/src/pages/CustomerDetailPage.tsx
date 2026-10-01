import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";
import { ArrowLeft, Pencil, Sparkles } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Card, CardContent } from "@/components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { ApprovalStatusBadge, NatureOfBusinessBadge } from "@/components/customers/CustomerBadges";
import { CustomerFormDialog } from "@/components/customers/CustomerFormDialog";
import { CustomerAuditTab } from "@/components/customers/CustomerAuditTab";
import { CustomerQuotationsTab } from "@/components/customers/CustomerQuotationsTab";
import { ComingSoon } from "@/components/common/ComingSoon";
import { customersApi } from "@/api/customers";
import { extractErrorMessage } from "@/api/client";
import { APPROVAL_STATUSES, APPROVAL_STATUS_LABELS, type ApprovalStatus } from "@/types/customer";

export function CustomerDetailPage() {
  const { customerId } = useParams<{ customerId: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [editOpen, setEditOpen] = useState(false);

  const customerQuery = useQuery({
    queryKey: ["customer", customerId],
    queryFn: () => customersApi.getById(customerId!),
    enabled: !!customerId,
  });

  const changeStatus = useMutation({
    mutationFn: (status: ApprovalStatus) => customersApi.changeApprovalStatus(customerId!, status),
    onSuccess: (updated) => {
      queryClient.setQueryData(["customer", customerId], updated);
      queryClient.invalidateQueries({ queryKey: ["customers"] });
      toast.success(`Marked as ${APPROVAL_STATUS_LABELS[updated.approvalStatus]}`);
    },
    onError: (error) => toast.error(extractErrorMessage(error, "Couldn't change approval status")),
  });

  if (customerQuery.isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-40 w-full" />
      </div>
    );
  }

  if (customerQuery.isError || !customerQuery.data) {
    return (
      <ComingSoon
        title="Customer not found"
        description="This customer may have been archived, or the link is stale."
      />
    );
  }

  const customer = customerQuery.data;

  return (
    <div>
      <Button
        variant="ghost"
        size="sm"
        className="mb-3 -ml-2 text-muted-foreground"
        onClick={() => navigate("/customers")}
      >
        <ArrowLeft className="h-4 w-4" />
        Back to customers
      </Button>

      <div className="mb-5 flex flex-wrap items-start justify-between gap-3">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-xl font-semibold tracking-tight">{customer.companyName}</h1>
            <NatureOfBusinessBadge nature={customer.natureOfBusiness} />
          </div>
          <p className="mt-0.5 text-sm text-muted-foreground">
            {customer.gstNo ?? "No GST on file"} · {customer.billingAddress.city ?? "No city set"} · Account
            manager: {customer.accountManagerName ?? "Unassigned"}
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Select value={customer.approvalStatus} onValueChange={(v) => changeStatus.mutate(v as ApprovalStatus)}>
            <SelectTrigger className="w-40">
              <SelectValue>
                <ApprovalStatusBadge status={customer.approvalStatus} />
              </SelectValue>
            </SelectTrigger>
            <SelectContent>
              {APPROVAL_STATUSES.map((s) => (
                <SelectItem key={s} value={s}>
                  {APPROVAL_STATUS_LABELS[s]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Button variant="outline" size="sm" onClick={() => setEditOpen(true)}>
            <Pencil className="h-4 w-4" />
            Edit
          </Button>
        </div>
      </div>

      {/* AI automation #4 - CustomerInsightService's rule-based health note */}
      {customer.aiHealthSummary && (
        <Card className="mb-5 border-primary/20 bg-primary/5">
          <CardContent className="flex items-start gap-2 p-4 text-sm">
            <Sparkles className="mt-0.5 h-4 w-4 shrink-0 text-primary" />
            <p>{customer.aiHealthSummary}</p>
          </CardContent>
        </Card>
      )}

      <Tabs defaultValue="overview">
        <TabsList>
          <TabsTrigger value="overview">Overview</TabsTrigger>
          <TabsTrigger value="contacts">Contacts</TabsTrigger>
          <TabsTrigger value="quotations">Quotations</TabsTrigger>
          <TabsTrigger value="documents">Documents</TabsTrigger>
          <TabsTrigger value="audit">Audit Log</TabsTrigger>
        </TabsList>

        <TabsContent value="overview">
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            <Card>
              <CardContent className="space-y-2 p-5 text-sm">
                <p className="mb-1 font-medium">Bill to address</p>
                <p className="text-muted-foreground">
                  {[customer.billingAddress.line1, customer.billingAddress.line2, customer.billingAddress.area]
                    .filter(Boolean)
                    .join(", ") || "—"}
                </p>
                <p className="text-muted-foreground">
                  {[customer.billingAddress.city, customer.billingAddress.state, customer.billingAddress.pinCode]
                    .filter(Boolean)
                    .join(", ") || "—"}
                </p>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="space-y-2 p-5 text-sm">
                <p className="mb-1 font-medium">Ship to address</p>
                {customer.shippingSameAsBilling ? (
                  <p className="text-muted-foreground">Same as billing address</p>
                ) : (
                  <>
                    <p className="text-muted-foreground">
                      {[customer.shippingAddress.line1, customer.shippingAddress.line2, customer.shippingAddress.area]
                        .filter(Boolean)
                        .join(", ") || "—"}
                    </p>
                    <p className="text-muted-foreground">
                      {[customer.shippingAddress.city, customer.shippingAddress.state, customer.shippingAddress.pinCode]
                        .filter(Boolean)
                        .join(", ") || "—"}
                    </p>
                  </>
                )}
              </CardContent>
            </Card>
          </div>
        </TabsContent>

        <TabsContent value="contacts">
          {customer.contacts.length === 0 ? (
            <ComingSoon title="No contacts yet" description="Edit this customer to add a contact." />
          ) : (
            <div className="divide-y divide-border rounded-lg border border-border">
              {customer.contacts.map((c) => (
                <div key={c.id} className="flex items-center justify-between gap-3 p-3 text-sm">
                  <div>
                    <p className="font-medium">
                      {c.name} <span className="font-normal text-muted-foreground">· {c.contactType}</span>
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {c.email ?? "—"} · {c.contactNo ?? "—"}
                    </p>
                  </div>
                  <p className="text-xs text-muted-foreground">{c.designation ?? ""}</p>
                </div>
              ))}
            </div>
          )}
        </TabsContent>

        <TabsContent value="quotations">
          <CustomerQuotationsTab customerId={customer.id} />
        </TabsContent>

        {/* Placeholder — ships as its own feature in a later chat. */}
        <TabsContent value="documents">
          <ComingSoon title="Documents" description="Uploaded customer documents will show up here." phase="Planned" />
        </TabsContent>

        <TabsContent value="audit">
          <CustomerAuditTab customerId={customer.id} />
        </TabsContent>
      </Tabs>

      <CustomerFormDialog
        open={editOpen}
        onOpenChange={setEditOpen}
        customer={customer}
        onSaved={(updated) => {
          queryClient.setQueryData(["customer", customerId], updated);
          queryClient.invalidateQueries({ queryKey: ["customers"] });
        }}
      />
    </div>
  );
}

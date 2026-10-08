import { useNavigate } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { ArrowRight, Building2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { leadConversionApi } from "@/api/leadConversion";
import { extractErrorMessage } from "@/api/client";
import { LEAD_STAGES, type LeadResponse } from "@/types/lead";

/** Mirrors LeadConversionService.isConvertible exactly: WON always qualifies,
 *  LOST never does, anything else needs to have reached Qualified. */
function isConvertible(stage: LeadResponse["stage"]): boolean {
  if (stage === "WON") return true;
  if (stage === "LOST") return false;
  return LEAD_STAGES.indexOf(stage) >= LEAD_STAGES.indexOf("QUALIFIED");
}

export function ConvertToCustomerButton({ lead }: { lead: LeadResponse }) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const eligible = isConvertible(lead.stage);

  const convertedQuery = useQuery({
    queryKey: ["lead-conversion", lead.id],
    queryFn: () => leadConversionApi.getConvertedCustomer(lead.id),
    enabled: eligible,
  });

  const convertMutation = useMutation({
    mutationFn: () => leadConversionApi.convertToCustomer(lead.id),
    onSuccess: (customer) => {
      toast.success(`${lead.fullName} converted to customer`);
      queryClient.setQueryData(["lead-conversion", lead.id], customer);
      queryClient.invalidateQueries({ queryKey: ["quotations"] });
    },
    onError: (error) => toast.error(extractErrorMessage(error, "Couldn't convert this lead")),
  });

  if (!eligible) return null;

  if (convertedQuery.data) {
    const customer = convertedQuery.data;
    return (
      <Button variant="outline" size="sm" onClick={() => navigate(`/customers/${customer.id}`)}>
        <Building2 className="h-4 w-4" />
        View customer
      </Button>
    );
  }

  return (
    <Button
      variant="outline"
      size="sm"
      disabled={convertMutation.isPending || convertedQuery.isLoading}
      onClick={() => convertMutation.mutate()}
    >
      <ArrowRight className="h-4 w-4" />
      {convertMutation.isPending ? "Converting…" : "Convert to customer"}
    </Button>
  );
}

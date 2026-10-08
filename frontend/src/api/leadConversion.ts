import { apiClient } from "./client";
import type { ApiResponse } from "@/types/api";
import type { CustomerResponse } from "@/types/customer";

/** Separate from api/leads.ts on purpose — mirrors the backend's standalone
 *  LeadConversionController, kept out of the main lead CRUD surface. */
export const leadConversionApi = {
  /** Null means this lead hasn't been converted yet. */
  async getConvertedCustomer(leadId: string): Promise<CustomerResponse | null> {
    const { data } = await apiClient.get<ApiResponse<CustomerResponse | null>>(`/leads/${leadId}/customer`);
    return data.data;
  },

  async convertToCustomer(leadId: string): Promise<CustomerResponse> {
    const { data } = await apiClient.post<ApiResponse<CustomerResponse>>(`/leads/${leadId}/convert-to-customer`);
    return data.data;
  },
};

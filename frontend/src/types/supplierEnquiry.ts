// Mirrors com.doctutorials.crm.supplier.enquiry.* — PRD 12.2's
// Customer Requirement -> Supplier Enquiry -> Supplier Reply -> chosen price chain.

export type SupplierEnquiryStatus = "OPEN" | "CLOSED";
export type SupplierReplyStatus = "PENDING" | "REPLIED" | "DECLINED";

export const SUPPLIER_REPLY_STATUS_LABELS: Record<SupplierReplyStatus, string> = {
  PENDING: "Awaiting reply",
  REPLIED: "Replied",
  DECLINED: "Declined",
};

// Mirrors SupplierEnquiryReplyResponse
export interface SupplierEnquiryReplyResponse {
  id: string;
  supplierId: string;
  supplierName: string;
  supplierContactEmail: string | null;
  status: SupplierReplyStatus;
  price: number | null;
  currency: string;
  validUntil: string | null;
  deliveryInfo: string | null;
  supplierReference: string | null;
  terms: string | null;
  remarks: string | null;
  chosen: boolean;
  emailSentAt: string | null;
  repliedAt: string | null;
}

// Mirrors SupplierEnquiryResponse
export interface SupplierEnquiryResponse {
  id: string;
  enquiryCode: string;
  leadId: string;
  leadName: string;
  /** Set when this enquiry is sourcing for a specific requirement - lets
   *  QuotationService.prefill find "the chosen price for this requirement". */
  requirementId: string | null;
  itemDescription: string;
  quantity: string | null;
  notes: string | null;
  status: SupplierEnquiryStatus;
  createdByName: string | null;
  createdAt: string;
  replies: SupplierEnquiryReplyResponse[];
}

// Mirrors SupplierEnquiryRequest
export interface SupplierEnquiryRequest {
  leadId: string;
  requirementId?: string | null;
  itemDescription: string;
  quantity?: string | null;
  notes?: string | null;
  supplierIds: string[];
}

// Mirrors SupplierEnquiryReplyRequest
export interface SupplierEnquiryReplyRequest {
  status?: SupplierReplyStatus;
  price?: number | null;
  currency?: string | null;
  validUntil?: string | null;
  deliveryInfo?: string | null;
  supplierReference?: string | null;
  terms?: string | null;
  remarks?: string | null;
}

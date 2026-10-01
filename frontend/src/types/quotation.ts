// Mirrors com.doctutorials.crm.quotation.* (PRD §13) — a customer-facing
// quotation with multiple line items, each optionally priced against a
// Supplier so the "price trail" from PRD §12.3 stays visible.

export type QuotationStatus =
  | "DRAFT"
  | "UNDER_REVIEW"
  | "SENT"
  | "VIEWED"
  | "NEGOTIATION"
  | "ACCEPTED"
  | "REJECTED"
  | "EXPIRED";

export const QUOTATION_STATUSES: QuotationStatus[] = [
  "DRAFT",
  "UNDER_REVIEW",
  "SENT",
  "VIEWED",
  "NEGOTIATION",
  "ACCEPTED",
  "REJECTED",
  "EXPIRED",
];

export const QUOTATION_STATUS_LABELS: Record<QuotationStatus, string> = {
  DRAFT: "Draft",
  UNDER_REVIEW: "Under Review",
  SENT: "Sent",
  VIEWED: "Viewed",
  NEGOTIATION: "Negotiation",
  ACCEPTED: "Accepted",
  REJECTED: "Rejected",
  EXPIRED: "Expired",
};

/** Statuses QuotationService blocks edits on — mirror LOCKED_FOR_EDITING there. */
export const QUOTATION_LOCKED_STATUSES: QuotationStatus[] = ["ACCEPTED", "REJECTED", "EXPIRED"];

// Mirrors com.doctutorials.crm.quotation.dto.QuotationLineItemResponse
export interface QuotationLineItemResponse {
  id: string;
  productName: string;
  description: string | null;
  unit: string;
  quantity: number;
  supplierId: string | null;
  supplierName: string | null;
  supplierPrice: number | null;
  sellingPrice: number;
  lineTotal: number;
  marginAmount: number | null;
  marginPercent: number | null;
  remarks: string | null;
}

// Mirrors com.doctutorials.crm.quotation.dto.QuotationLineItemRequest
export interface QuotationLineItemRequest {
  id?: string | null;
  productName: string;
  description?: string | null;
  unit: string;
  quantity: number;
  supplierId?: string | null;
  supplierPrice?: number | null;
  sellingPrice: number;
  remarks?: string | null;
}

// Mirrors com.doctutorials.crm.quotation.dto.QuotationResponse
export interface QuotationResponse {
  id: string;
  quotationNumber: string;
  customerId: string;
  customerName: string;
  customerContactId: string | null;
  customerContactName: string | null;
  preparedById: string | null;
  preparedByName: string | null;
  quotationDate: string;
  validUntil: string | null;
  status: QuotationStatus;
  currency: string;
  taxPercent: number | null;
  termsAndConditions: string | null;
  remarks: string | null;
  lineItems: QuotationLineItemResponse[];
  subtotal: number;
  taxAmount: number;
  grandTotal: number;
  totalMarginAmount: number;
  createdAt: string;
  updatedAt: string;
}

// Mirrors com.doctutorials.crm.quotation.dto.QuotationRequest
export interface QuotationRequest {
  customerId: string;
  customerContactId?: string | null;
  quotationDate?: string | null;
  validUntil?: string | null;
  currency?: string | null;
  taxPercent?: number | null;
  termsAndConditions?: string | null;
  remarks?: string | null;
  lineItems: QuotationLineItemRequest[];
}

export interface QuotationListFilters {
  q?: string;
  customerId?: string;
  status?: QuotationStatus;
  page?: number;
  size?: number;
}

// Mirrors com.doctutorials.crm.quotation.QuotationService.QuotationSummary
export interface QuotationSummary {
  total: number;
  draft: number;
  pending: number;
  accepted: number;
  rejectedOrExpired: number;
  acceptedValue: number;
}

// Mirrors com.doctutorials.crm.quotation.* (PRD §12.3, §13) — a quotation for
// EITHER a Customer or an active Lead (exactly one), with multiple line items
// each optionally priced against a Supplier / a chosen supplier-enquiry-reply
// so the "price trail" from PRD §12.3 stays visible.

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

/** Statuses QuotationService blocks content edits on — mirror EDITABLE there (anything NOT in this list is locked). */
export const QUOTATION_LOCKED_STATUSES: QuotationStatus[] = [
  "SENT",
  "VIEWED",
  "NEGOTIATION",
  "ACCEPTED",
  "REJECTED",
  "EXPIRED",
];

/** Mirrors QuotationService.ALLOWED_TRANSITIONS exactly — the UI uses this to only ever
 *  offer a status change that the server will actually accept. EXPIRED is reachable here
 *  (manual override) even though it's also set automatically by the nightly expiry job. */
export const QUOTATION_NEXT_STATUSES: Record<QuotationStatus, QuotationStatus[]> = {
  DRAFT: ["UNDER_REVIEW"],
  UNDER_REVIEW: ["DRAFT", "SENT"],
  SENT: ["VIEWED", "NEGOTIATION", "ACCEPTED", "REJECTED", "EXPIRED"],
  VIEWED: ["NEGOTIATION", "ACCEPTED", "REJECTED", "EXPIRED"],
  NEGOTIATION: ["ACCEPTED", "REJECTED", "EXPIRED"],
  ACCEPTED: [],
  REJECTED: [],
  EXPIRED: [],
};

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
  /** Non-null when this line's supplier/price came from a chosen supplier-enquiry
   *  reply rather than being typed by hand — show supplierReference/enquiryCode as a
   *  read-only "price source" and lock the supplier price field when set. */
  enquiryReplyId: string | null;
  supplierReference: string | null;
  enquiryCode: string | null;
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
  /** When set, supplierId/supplierPrice above are ignored server-side — see
   *  QuotationService.applyLineItemPricing. */
  enquiryReplyId?: string | null;
  remarks?: string | null;
}

// Mirrors com.doctutorials.crm.quotation.dto.QuotationResponse
export interface QuotationResponse {
  id: string;
  quotationNumber: string;
  customerId: string | null;
  customerName: string | null;
  leadId: string | null;
  leadName: string | null;
  requirementId: string | null;
  requirementProductName: string | null;
  /** customerName if there's a customer, else leadName — always safe to render. */
  displayName: string;
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
  /** True when ACCEPTED and linked to a lead — show the "mark the lead as Won" banner.
   *  The server never flips the lead to Won on its own; this is a prompt, not a result. */
  canMarkLeadWon: boolean;
  createdAt: string;
  updatedAt: string;
}

// Mirrors com.doctutorials.crm.quotation.dto.QuotationRequest
export interface QuotationRequest {
  /** Exactly one of customerId/leadId must be given. */
  customerId?: string | null;
  leadId?: string | null;
  requirementId?: string | null;
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

// Mirrors com.doctutorials.crm.quotation.dto.QuotationPrefillResponse
export interface QuotationPrefillResponse {
  leadId: string;
  leadName: string;
  /** Set if the lead has already been converted — lets the builder default to the customer. */
  customerId: string | null;
  productName: string;
  quantity: number;
  unit: string;
  currency: string;
  supplierId: string;
  supplierName: string;
  supplierPrice: number;
  enquiryReplyId: string;
  supplierReference: string | null;
  enquiryCode: string;
  replyValidUntil: string | null;
}

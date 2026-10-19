// Mirrors com.doctutorials.crm.supplier.enquiry.dto.* field for field.
// Backend serializes with default-property-inclusion: non_null, so any
// nullable Java field may be ABSENT in JSON, not just null.

export type SupplierEnquiryStatus = "OPEN" | "CLOSED";
export type SupplierReplyStatus = "PENDING" | "REPLIED" | "DECLINED";
export type SupplierStatus = "PENDING_VERIFICATION" | "VERIFIED" | "ON_HOLD" | "BLACKLISTED";

export const SUPPLIER_REPLY_STATUS_LABELS: Record<SupplierReplyStatus, string> = {
  PENDING: "Awaiting reply",
  REPLIED: "Replied",
  DECLINED: "Declined",
};

// SupplierEnquiryReplyResponse
export interface SupplierEnquiryReplyResponse {
  id: string;
  enquiryId: string;
  requirementId?: string | null;
  supplierId: string;
  supplierCode: string;
  supplierName: string;
  supplierContactEmail?: string | null;
  supplierStatus: SupplierStatus;
  status: SupplierReplyStatus;
  emailSent: boolean;
  emailError?: string | null;
  emailSentAt?: string | null;
  price?: number | null;
  currency: string;
  validUntil?: string | null; // yyyy-MM-dd
  expired: boolean;
  deliveryInfo?: string | null;
  supplierReference?: string | null;
  terms?: string | null;
  remarks?: string | null;
  chosen: boolean;
  repliedAt?: string | null;
  // Only populated on GET /supplier-enquiries/by-supplier/{id}
  enquiryCode?: string | null;
  itemDescription?: string | null;
  leadName?: string | null;
}

// SupplierEnquiryResponse
export interface SupplierEnquiryResponse {
  id: string;
  enquiryCode: string;
  requirementId?: string | null; // null on enquiries raised before requirements were linked
  leadId: string;
  leadName: string;
  itemDescription: string;
  quantity?: string | null;
  notes?: string | null;
  status: SupplierEnquiryStatus;
  createdByName?: string | null;
  createdAt: string;
  responseDueDate?: string | null;
  replies: SupplierEnquiryReplyResponse[];
}

// SupplierEnquiryRequest: enquiries are ALWAYS created from a requirement.
export interface SupplierEnquiryRequest {
  requirementId: string;
  supplierIds: string[]; // 1..20
  message?: string | null;
  responseDueDate?: string | null;
}

// SupplierEnquiryReplyRequest
export interface SupplierEnquiryReplyRequest {
  status: SupplierReplyStatus; // required by the backend (@NotNull)
  price?: number | null;
  currency?: string | null;
  validUntil?: string | null;
  deliveryInfo?: string | null;
  supplierReference?: string | null;
  terms?: string | null;
  remarks?: string | null;
}
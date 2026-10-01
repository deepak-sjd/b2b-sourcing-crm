// Mirrors com.doctutorials.crm.supplier.* — vendor/procurement master data,
// distinct from Lead (who we sell to) vs. Supplier (who we buy from).

export type SupplierCategory =
  | "MANUFACTURER"
  | "DISTRIBUTOR"
  | "WHOLESALER"
  | "SERVICE_PROVIDER"
  | "RETAILER"
  | "OTHER";

export const SUPPLIER_CATEGORIES: SupplierCategory[] = [
  "MANUFACTURER",
  "DISTRIBUTOR",
  "WHOLESALER",
  "SERVICE_PROVIDER",
  "RETAILER",
  "OTHER",
];

export const SUPPLIER_CATEGORY_LABELS: Record<SupplierCategory, string> = {
  MANUFACTURER: "Manufacturer",
  DISTRIBUTOR: "Distributor",
  WHOLESALER: "Wholesaler",
  SERVICE_PROVIDER: "Service Provider",
  RETAILER: "Retailer",
  OTHER: "Other",
};

export type SupplierStatus = "PENDING_VERIFICATION" | "VERIFIED" | "ON_HOLD" | "BLACKLISTED";

export const SUPPLIER_STATUSES: SupplierStatus[] = [
  "PENDING_VERIFICATION",
  "VERIFIED",
  "ON_HOLD",
  "BLACKLISTED",
];

export const SUPPLIER_STATUS_LABELS: Record<SupplierStatus, string> = {
  PENDING_VERIFICATION: "Pending Verification",
  VERIFIED: "Verified",
  ON_HOLD: "On Hold",
  BLACKLISTED: "Blacklisted",
};

export type PaymentTerms = "ADVANCE" | "COD" | "NET_15" | "NET_30" | "NET_45" | "NET_60";

export const PAYMENT_TERMS: PaymentTerms[] = ["ADVANCE", "COD", "NET_15", "NET_30", "NET_45", "NET_60"];

export const PAYMENT_TERMS_LABELS: Record<PaymentTerms, string> = {
  ADVANCE: "Advance",
  COD: "Cash on Delivery",
  NET_15: "Net 15",
  NET_30: "Net 30",
  NET_45: "Net 45",
  NET_60: "Net 60",
};

// Mirrors com.doctutorials.crm.supplier.dto.SupplierResponse
export interface SupplierResponse {
  id: string;
  supplierCode: string;
  companyName: string;
  category: SupplierCategory;
  status: SupplierStatus;
  gstNumber: string | null;
  gstVerified: boolean;
  panNumber: string | null;
  addressLine1: string;
  addressLine2: string | null;
  country: string;
  state: string | null;
  city: string | null;
  pincode: string | null;
  bankName: string | null;
  bankAddress: string | null;
  accountHolderName: string | null;
  accountNumber: string | null;
  ifscCode: string | null;
  contactName: string | null;
  contactDesignation: string | null;
  contactEmail: string | null;
  contactPhone: string | null;
  paymentTerms: PaymentTerms | null;
  products: string[];
  createdByName: string | null;
  trustScore: number;
  scoreNote: string | null;
  createdAt: string;
  updatedAt: string;
}

// Mirrors com.doctutorials.crm.supplier.dto.SupplierRequest
export interface SupplierRequest {
  companyName: string;
  category: SupplierCategory;
  gstNumber?: string | null;
  panNumber?: string | null;
  addressLine1: string;
  addressLine2?: string | null;
  country?: string | null;
  state?: string | null;
  city?: string | null;
  pincode?: string | null;
  bankName?: string | null;
  bankAddress?: string | null;
  accountHolderName?: string | null;
  accountNumber?: string | null;
  ifscCode?: string | null;
  contactName?: string | null;
  contactDesignation?: string | null;
  contactEmail?: string | null;
  contactPhone?: string | null;
  paymentTerms?: PaymentTerms | null;
  products: string[];
}

export interface SupplierListFilters {
  q?: string;
  status?: SupplierStatus;
  category?: SupplierCategory;
  page?: number;
  size?: number;
}

// Mirrors com.doctutorials.crm.supplier.SupplierService.SupplierSummary
export interface SupplierSummary {
  total: number;
  verified: number;
  pendingVerification: number;
  onHoldOrBlacklisted: number;
  averageTrustScore: number;
}

// Mirrors com.doctutorials.crm.supplier.dto.GstLookupResponse
export interface GstLookupResponse {
  structureValid: boolean;
  checksumValid: boolean;
  stateCode: string | null;
  stateName: string | null;
  panNumber: string | null;
}

// Mirrors com.doctutorials.crm.supplier.SupplierDuplicateService.DuplicateMatch
export interface SupplierDuplicateMatch {
  id: string;
  companyName: string;
  reason: string;
  confidencePercent: number;
}

// Mirrors com.doctutorials.crm.customer.NatureOfBusiness
export type NatureOfBusiness =
  | "MANUFACTURER"
  | "TRADER"
  | "SERVICE_PROVIDER"
  | "DISTRIBUTOR"
  | "RETAILER"
  | "OTHER";

export const NATURE_OF_BUSINESS_OPTIONS: NatureOfBusiness[] = [
  "MANUFACTURER",
  "TRADER",
  "SERVICE_PROVIDER",
  "DISTRIBUTOR",
  "RETAILER",
  "OTHER",
];

export const NATURE_OF_BUSINESS_LABELS: Record<NatureOfBusiness, string> = {
  MANUFACTURER: "Manufacturer",
  TRADER: "Trader",
  SERVICE_PROVIDER: "Service Provider",
  DISTRIBUTOR: "Distributor",
  RETAILER: "Retailer",
  OTHER: "Other",
};

// Mirrors com.doctutorials.crm.customer.ApprovalStatus
export type ApprovalStatus = "PENDING" | "APPROVED" | "REJECTED";
export const APPROVAL_STATUSES: ApprovalStatus[] = ["PENDING", "APPROVED", "REJECTED"];
export const APPROVAL_STATUS_LABELS: Record<ApprovalStatus, string> = {
  PENDING: "Pending",
  APPROVED: "Approved",
  REJECTED: "Rejected",
};

// Mirrors com.doctutorials.crm.customer.dto.AddressDto
export interface Address {
  line1: string;
  line2?: string | null;
  area?: string | null;
  country?: string | null;
  state?: string | null;
  city?: string | null;
  pinCode?: string | null;
  phone?: string | null;
  email?: string | null;
  gstNo?: string | null;
}

export const emptyAddress: Address = {
  line1: "",
  line2: "",
  area: "",
  country: "India",
  state: "",
  city: "",
  pinCode: "",
  phone: "",
  email: "",
  gstNo: "",
};

// Mirrors com.doctutorials.crm.customer.dto.CustomerContactRequest / Response
export interface CustomerContact {
  id?: string;
  contactType: string;
  name: string;
  email?: string | null;
  contactNo?: string | null;
  designation?: string | null;
}

// Mirrors com.doctutorials.crm.customer.dto.CustomerResponse
export interface CustomerResponse {
  id: string;
  companyName: string;
  gstNo: string | null;
  natureOfBusiness: NatureOfBusiness | null;
  accountManagerId: string | null;
  accountManagerName: string | null;
  billingAddress: Address;
  shippingAddress: Address;
  shippingSameAsBilling: boolean;
  approvalStatus: ApprovalStatus;
  active: boolean;
  convertedFromLeadId: string | null;
  aiHealthSummary: string | null;
  aiHealthGeneratedAt: string | null;
  contacts: CustomerContact[];
  createdAt: string;
}

// Mirrors com.doctutorials.crm.customer.dto.CustomerRequest
export interface CustomerRequest {
  companyName: string;
  gstNo?: string | null;
  natureOfBusiness?: NatureOfBusiness | null;
  accountManagerId?: string | null;
  billingAddress: Address;
  shippingAddress?: Address | null;
  shippingSameAsBilling: boolean;
  approvalStatus?: ApprovalStatus;
  contacts: CustomerContact[];
}

export interface CustomerListFilters {
  approvalStatus?: ApprovalStatus;
  accountManagerId?: string;
  page?: number;
  size?: number;
}

// Mirrors CustomerDuplicateService.DuplicateMatch
export interface DuplicateMatch {
  id: string;
  companyName: string;
  matchedOn: string;
}

// Mirrors GstStateResolver.GstInfo
export interface GstInfo {
  valid: boolean;
  stateCode: string | null;
  state: string | null;
}

// Mirrors AccountManagerSuggestionService.Suggestion
export interface AccountManagerSuggestion {
  userId: string;
  fullName: string;
  currentLoad: number;
}

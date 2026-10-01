// Mirrors com.doctutorials.crm.contact.ContactEngagement
export type ContactEngagement = "NEW" | "ACTIVE" | "COOLING" | "AT_RISK";

export const CONTACT_ENGAGEMENTS: ContactEngagement[] = ["NEW", "ACTIVE", "COOLING", "AT_RISK"];

export const CONTACT_ENGAGEMENT_LABELS: Record<ContactEngagement, string> = {
  NEW: "New",
  ACTIVE: "Active",
  COOLING: "Cooling",
  AT_RISK: "At risk",
};

// Mirrors com.doctutorials.crm.contact.dto.ContactResponse
export interface ContactResponse {
  id: string;
  fullName: string;
  designation: string | null;
  company: string | null;
  phone: string | null;
  email: string | null;
  city: string | null;
  source: string | null;
  ownerId: string | null;
  ownerName: string | null;
  createdByName: string | null;
  lastActivityAt: string | null;
  nextFollowUpAt: string | null;
  daysSinceLastActivity: number;
  emailBounced: boolean;
  engagementScore: number;
  engagement: ContactEngagement;
  aiInsight: string | null;
  createdAt: string;
}

// Mirrors com.doctutorials.crm.contact.dto.ContactRequest
export interface ContactRequest {
  fullName: string;
  designation?: string | null;
  company?: string | null;
  phone?: string | null;
  email?: string | null;
  city?: string | null;
  source?: string | null;
  ownerId?: string | null;
  emailBounced: boolean;
}

// Mirrors com.doctutorials.crm.contact.dto.ContactStatsResponse
export interface ContactStatsResponse {
  total: number;
  active: number;
  cooling: number;
  atRisk: number;
  bounced: number;
  unassigned: number;
}

// Mirrors com.doctutorials.crm.contact.dto.DuplicateCheckResponse
export interface DuplicateCheckResponse {
  duplicate: boolean;
  match: ContactResponse | null;
  matchedOn: "phone" | "email" | null;
}

export interface ContactListFilters {
  search?: string;
  engagement?: ContactEngagement;
  ownerId?: string;
  page?: number;
  size?: number;
}

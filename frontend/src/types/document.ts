import type { EntityUserRef } from "./common";

// Mirrors com.doctutorials.crm.document.DocumentType
export type DocumentType =
  | "CUSTOMER_DOCUMENT" | "SUPPLIER_QUOTATION" | "CUSTOMER_QUOTATION"
  | "PRODUCT_DOCUMENT" | "MEETING_DOCUMENT" | "REQUIREMENT_DOCUMENT" | "OTHER";

export const DOCUMENT_TYPES: DocumentType[] = [
  "CUSTOMER_DOCUMENT", "SUPPLIER_QUOTATION", "CUSTOMER_QUOTATION",
  "PRODUCT_DOCUMENT", "MEETING_DOCUMENT", "REQUIREMENT_DOCUMENT", "OTHER",
];

export const DOCUMENT_TYPE_LABELS: Record<DocumentType, string> = {
  CUSTOMER_DOCUMENT: "Customer document",
  SUPPLIER_QUOTATION: "Supplier quotation",
  CUSTOMER_QUOTATION: "Customer quotation",
  PRODUCT_DOCUMENT: "Product document",
  MEETING_DOCUMENT: "Meeting document",
  REQUIREMENT_DOCUMENT: "Requirement document",
  OTHER: "Other",
};

// Mirrors com.doctutorials.crm.document.DocumentParentType
export type DocumentParentType = "LEAD" | "CUSTOMER" | "SUPPLIER" | "REQUIREMENT" | "QUOTATION" | "ENQUIRY_REPLY";

// Mirrors com.doctutorials.crm.document.Document entity as serialized by Jackson
export interface CrmDocument {
  id: string;
  uploadedBy: EntityUserRef | null;
  documentType: DocumentType;
  originalFileName: string;
  contentType: string;
  sizeBytes: number;
  description: string | null;
  createdAt: string;
}
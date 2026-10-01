import type { EntityUserRef } from "./common";

// Mirrors com.doctutorials.crm.document.DocumentType
export type DocumentType = "ID_PROOF" | "MARKSHEET" | "PAYMENT_RECEIPT" | "AGREEMENT" | "OTHER";
export const DOCUMENT_TYPES: DocumentType[] = [
  "ID_PROOF",
  "MARKSHEET",
  "PAYMENT_RECEIPT",
  "AGREEMENT",
  "OTHER",
];
export const DOCUMENT_TYPE_LABELS: Record<DocumentType, string> = {
  ID_PROOF: "ID proof",
  MARKSHEET: "Marksheet",
  PAYMENT_RECEIPT: "Payment receipt",
  AGREEMENT: "Agreement",
  OTHER: "Other",
};

// Mirrors com.doctutorials.crm.document.Document entity as serialized by Jackson
export interface CrmDocument {
  id: string;
  uploadedBy: EntityUserRef | null;
  documentType: DocumentType;
  originalFileName: string;
  contentType: string;
  sizeBytes: number;
  createdAt: string;
}

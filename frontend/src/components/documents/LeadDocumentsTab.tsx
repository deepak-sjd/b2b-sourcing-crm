import { AttachmentsPanel } from "./AttachmentsPanel";

export function LeadDocumentsTab({ leadId }: { leadId: string }) {
  return <AttachmentsPanel parentType="LEAD" parentId={leadId} />;
}
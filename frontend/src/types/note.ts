import type { EntityLeadRef, EntityUserRef } from "./common";

// Mirrors com.doctutorials.crm.note.Note entity as serialized by Jackson
export interface Note {
  id: string;
  lead: EntityLeadRef;
  author: EntityUserRef | null;
  content: string;
  createdAt: string;
  updatedAt: string;
}

// Mirrors NoteController.AddNoteRequest
export interface AddNoteRequest {
  leadId: string;
  authorId?: string | null;
  content: string;
}

import { Plus, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import type { CustomerContact } from "@/types/customer";

// Suggested values only — CustomerContact.contactType is free text on the
// backend (see CustomerContact.java), so a client can type their own label.
const CONTACT_TYPE_SUGGESTIONS = ["Primary", "Billing", "Technical", "Decision Maker"];

interface CustomerContactsCardProps {
  contacts: CustomerContact[];
  onChange: (contacts: CustomerContact[]) => void;
}

export function CustomerContactsCard({ contacts, onChange }: CustomerContactsCardProps) {
  function update(index: number, patch: Partial<CustomerContact>) {
    onChange(contacts.map((c, i) => (i === index ? { ...c, ...patch } : c)));
  }

  function add() {
    onChange([...contacts, { contactType: "Primary", name: "", email: "", contactNo: "", designation: "" }]);
  }

  function remove(index: number) {
    onChange(contacts.filter((_, i) => i !== index));
  }

  return (
    <div className="space-y-3">
      {contacts.length === 0 && (
        <p className="text-sm text-muted-foreground">No contacts added yet.</p>
      )}
      {contacts.map((contact, i) => (
        <div key={contact.id ?? `new-${i}`} className="grid grid-cols-2 gap-3 rounded-md border border-border p-3">
          <div className="col-span-2 flex items-center justify-between">
            <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">Contact {i + 1}</p>
            <Button type="button" variant="ghost" size="icon" onClick={() => remove(i)} aria-label="Remove contact">
              <Trash2 className="h-4 w-4 text-destructive" />
            </Button>
          </div>
          <div className="space-y-1.5">
            <Label>Contact type</Label>
            <Select value={contact.contactType} onValueChange={(v) => update(i, { contactType: v })}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {CONTACT_TYPE_SUGGESTIONS.map((t) => (
                  <SelectItem key={t} value={t}>
                    {t}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="space-y-1.5">
            <Label>Name</Label>
            <Input value={contact.name} onChange={(e) => update(i, { name: e.target.value })} />
          </div>
          <div className="space-y-1.5">
            <Label>Email</Label>
            <Input type="email" value={contact.email ?? ""} onChange={(e) => update(i, { email: e.target.value })} />
          </div>
          <div className="space-y-1.5">
            <Label>Contact no.</Label>
            <Input value={contact.contactNo ?? ""} onChange={(e) => update(i, { contactNo: e.target.value })} />
          </div>
          <div className="col-span-2 space-y-1.5">
            <Label>Designation</Label>
            <Input value={contact.designation ?? ""} onChange={(e) => update(i, { designation: e.target.value })} />
          </div>
        </div>
      ))}
      <Button type="button" variant="outline" size="sm" onClick={add}>
        <Plus className="h-4 w-4" />
        Add contact
      </Button>
    </div>
  );
}

import { AlertTriangle } from "lucide-react";
import type { DuplicateMatch } from "@/types/customer";

/** AI automation #1 (backed by CustomerDuplicateService) — shown live in the
 *  create/edit form as the user types the company name or GST number. Purely
 *  a warning, never a block: the user can still save through it. */
export function DuplicateWarningBanner({ matches }: { matches: DuplicateMatch[] }) {
  if (matches.length === 0) return null;

  return (
    <div className="flex items-start gap-2 rounded-md border border-amber-200 bg-amber-50 p-3 text-sm text-amber-900">
      <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" />
      <div>
        <p className="font-medium">This might already exist</p>
        <ul className="mt-1 space-y-0.5">
          {matches.map((m) => (
            <li key={m.id}>
              <span className="font-medium">{m.companyName}</span> — {m.matchedOn}
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}

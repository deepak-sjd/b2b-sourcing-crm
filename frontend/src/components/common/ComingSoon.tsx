import { Construction } from "lucide-react";
import { Badge } from "@/components/ui/badge";

interface ComingSoonProps {
  title: string;
  description: string;
  /** Which FEATURES.md phase this lands in, shown as a small badge so it's
   * clear this is a scoped, planned gap — not a bug. */
  phase?: string;
}

/** Used for any nav section / lead-detail tab that the client's screenshots
 * show but the backend doesn't implement yet (see FEATURES.md 🔲 items).
 * Keeping every planned surface visible — rather than hiding the nav item —
 * matches the client's mockups today and gives an obvious slot to wire up
 * real data into later, without a frontend rebuild. */
export function ComingSoon({ title, description, phase }: ComingSoonProps) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 rounded-lg border border-dashed border-border py-16 text-center">
      <div className="flex h-12 w-12 items-center justify-center rounded-full bg-accent">
        <Construction className="h-5 w-5 text-accent-foreground" />
      </div>
      <div className="space-y-1">
        <div className="flex items-center justify-center gap-2">
          <p className="text-sm font-medium">{title}</p>
          {phase && <Badge variant="outline">{phase}</Badge>}
        </div>
        <p className="mx-auto max-w-sm text-sm text-muted-foreground">{description}</p>
      </div>
    </div>
  );
}

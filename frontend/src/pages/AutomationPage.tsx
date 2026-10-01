import { PageHeader } from "@/components/common/PageHeader";
import { Card, CardContent } from "@/components/ui/card";
import { ComingSoon } from "@/components/common/ComingSoon";

export function AutomationPage() {
  return (
    <div>
      <PageHeader title="Automation" description="Workflow rules that act on leads automatically." />
      <Card>
        <CardContent className="p-5">
          <ComingSoon
            title="Workflow automation"
            description="Trigger-based rules (e.g. auto-assign new leads, follow-up reminders, stage-change notifications) aren't built on the backend yet."
            phase="Phase 3"
          />
        </CardContent>
      </Card>
    </div>
  );
}

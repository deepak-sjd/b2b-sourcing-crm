import { PageHeader } from "@/components/common/PageHeader";
import { Card, CardContent } from "@/components/ui/card";
import { ComingSoon } from "@/components/common/ComingSoon";

export function WhatsAppPage() {
  return (
    <div>
      <PageHeader title="WhatsApp" description="Two-way WhatsApp conversations with leads." />
      <Card>
        <CardContent className="p-5">
          <ComingSoon
            title="WhatsApp Business API integration"
            description="Not started on the backend yet — see FEATURES.md. Once wired up, conversations will thread per lead (also surfaced in the lead detail's WhatsApp tab)."
            phase="Phase 2"
          />
        </CardContent>
      </Card>
    </div>
  );
}

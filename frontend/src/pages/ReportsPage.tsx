import { useRef, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { toast } from "sonner";
import { Send, Sparkles, Wrench } from "lucide-react";
import { PageHeader } from "@/components/common/PageHeader";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/common/EmptyState";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { ComingSoon } from "@/components/common/ComingSoon";
import { reportsApi } from "@/api/reports";
import { extractErrorMessage } from "@/api/client";
import { REPORT_GROUP_BY_OPTIONS, type ReportGroupBy } from "@/types/report";

function ReportBuilderTab() {
  const [groupBy, setGroupBy] = useState<ReportGroupBy>("NONE");
  const [ran, setRan] = useState(false);

  const reportQuery = useQuery({
    queryKey: ["reports", "leads", groupBy],
    queryFn: () => reportsApi.leadReport(groupBy),
    enabled: ran,
  });

  const rows = reportQuery.data ? Object.entries(reportQuery.data) : [];

  return (
    <div className="grid grid-cols-1 gap-4 lg:grid-cols-[280px,1fr]">
      <Card>
        <CardContent className="space-y-4 p-5">
          <h3 className="text-sm font-semibold">Configuration</h3>
          <div className="space-y-1.5">
            <label className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
              Data Source (Entity)
            </label>
            <Select value="LEADS_PIPELINE" disabled>
              <SelectTrigger>
                <SelectValue>Leads Pipeline</SelectValue>
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="LEADS_PIPELINE">Leads Pipeline</SelectItem>
              </SelectContent>
            </Select>
            <p className="text-xs text-muted-foreground">
              Tasks & Activity Log sources aren't wired up on the backend yet.
            </p>
          </div>
          <div className="space-y-1.5">
            <label className="text-xs font-medium uppercase tracking-wide text-muted-foreground">Group By</label>
            <Select value={groupBy} onValueChange={(v) => setGroupBy(v as ReportGroupBy)}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {REPORT_GROUP_BY_OPTIONS.map((o) => (
                  <SelectItem key={o.value} value={o.value}>
                    {o.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <Button className="w-full" onClick={() => setRan(true)}>
            <Wrench className="h-4 w-4" />
            Run report
          </Button>
        </CardContent>
      </Card>

      <Card>
        <CardContent className="p-5">
          <h3 className="mb-3 text-sm font-semibold">Results Preview</h3>
          {!ran ? (
            <EmptyState title="Configure and run a report to see results here" />
          ) : reportQuery.isLoading ? (
            <div className="space-y-2">
              <Skeleton className="h-8 w-full" />
              <Skeleton className="h-8 w-full" />
              <Skeleton className="h-8 w-full" />
            </div>
          ) : rows.length === 0 ? (
            <EmptyState title="No data for this grouping" />
          ) : (
            <div className="divide-y divide-border">
              {rows.map(([label, count]) => (
                <div key={label} className="flex items-center justify-between py-2 text-sm">
                  <span>{label}</span>
                  <span className="font-semibold">{count}</span>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

interface ChatMessage {
  role: "user" | "assistant";
  content: string;
}

function AiReportingAssistantTab() {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [question, setQuestion] = useState("");
  const scrollRef = useRef<HTMLDivElement>(null);

  const ask = useMutation({
    mutationFn: (q: string) => reportsApi.ask(q),
    onSuccess: (answer) => {
      setMessages((prev) => [...prev, { role: "assistant", content: answer.narrative }]);
      requestAnimationFrame(() => scrollRef.current?.scrollTo(0, scrollRef.current.scrollHeight));
    },
    onError: (error) => {
      toast.error(extractErrorMessage(error, "Couldn't get an answer"));
      setMessages((prev) => prev.slice(0, -1));
    },
  });

  function handleAsk() {
    const q = question.trim();
    if (!q || ask.isPending) return;
    setMessages((prev) => [...prev, { role: "user", content: q }]);
    setQuestion("");
    ask.mutate(q);
  }

  return (
    <Card>
      <CardContent className="flex h-[520px] flex-col p-0">
        <div ref={scrollRef} className="flex-1 space-y-3 overflow-y-auto p-5 thin-scrollbar">
          {messages.length === 0 ? (
            <EmptyState
              icon={Sparkles}
              title="Ask natural-language questions about your CRM data"
              description='Try "What is our overall conversion rate?" or "Who are the top performers this month?"'
            />
          ) : (
            messages.map((m, i) => (
              <div
                key={i}
                className={`max-w-[85%] rounded-lg px-3 py-2 text-sm whitespace-pre-wrap ${
                  m.role === "user" ? "ml-auto bg-primary text-primary-foreground" : "bg-muted"
                }`}
              >
                {m.content}
              </div>
            ))
          )}
          {ask.isPending && <div className="max-w-[85%] rounded-lg bg-muted px-3 py-2 text-sm text-muted-foreground">Thinking…</div>}
        </div>
        <div className="flex items-center gap-2 border-t border-border p-3">
          <Input
            placeholder="Ask a question about your CRM data…"
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") handleAsk();
            }}
          />
          <Button onClick={handleAsk} disabled={!question.trim() || ask.isPending}>
            <Send className="h-4 w-4" />
            Ask
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}

export function ReportsPage() {
  return (
    <div>
      <PageHeader title="Custom Reports & Analytics" description="Build, save, and analyze your CRM data, or ask the AI Assistant." />
      <Tabs defaultValue="builder">
        <TabsList>
          <TabsTrigger value="saved">Saved Reports</TabsTrigger>
          <TabsTrigger value="builder">Report Builder</TabsTrigger>
          <TabsTrigger value="ai">
            <Sparkles className="mr-1.5 h-3.5 w-3.5" />
            AI Reporting Assistant
          </TabsTrigger>
        </TabsList>
        <TabsContent value="saved">
          <ComingSoon
            title="Saved reports"
            description="Save a Report Builder configuration to re-run it later. Needs a small backend endpoint to persist report definitions."
            phase="Phase 2"
          />
        </TabsContent>
        <TabsContent value="builder">
          <ReportBuilderTab />
        </TabsContent>
        <TabsContent value="ai">
          <AiReportingAssistantTab />
        </TabsContent>
      </Tabs>
    </div>
  );
}

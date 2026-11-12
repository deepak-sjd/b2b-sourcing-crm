import { useRef, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Download, Upload } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { leadsApi } from "@/api/leads";
import { extractErrorMessage } from "@/api/client";
import type { LeadImportResult } from "@/types/lead";

const TEMPLATE =
  "name,phone,email,city,interest,budget,priority,source,campaign\n" +
  "Asha Rao,9876543210,asha@example.com,Patna,Premium plan,50000,MEDIUM,Referral,Diwali\n";

export function LeadImportDialog({ open, onOpenChange }: { open: boolean; onOpenChange: (o: boolean) => void }) {
  const queryClient = useQueryClient();
  const fileRef = useRef<HTMLInputElement>(null);
  const [file, setFile] = useState<File | null>(null);
  const [result, setResult] = useState<LeadImportResult | null>(null);

  const upload = useMutation({
    mutationFn: (f: File) => leadsApi.importCsv(f),
    onSuccess: (r) => {
      setResult(r);
      queryClient.invalidateQueries({ queryKey: ["leads"] });
      toast.success(`${r.created} lead(s) imported`);
    },
    onError: (error) => toast.error(extractErrorMessage(error, "Import failed")),
  });

  function handleOpenChange(next: boolean) {
    if (!next) {
      setFile(null);
      setResult(null);
      if (fileRef.current) fileRef.current.value = "";
    }
    onOpenChange(next);
  }

  function downloadTemplate() {
    const url = URL.createObjectURL(new Blob([TEMPLATE], { type: "text/csv" }));
    const a = document.createElement("a");
    a.href = url;
    a.download = "leads-template.csv";
    a.click();
    URL.revokeObjectURL(url);
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>Import leads</DialogTitle>
          <DialogDescription>
            Upload a CSV file (in Excel: Save As → CSV). Only the name column is mandatory, but each lead
            needs a phone or email. Duplicates are skipped.
          </DialogDescription>
        </DialogHeader>

        {!result ? (
          <div className="space-y-3">
            <Button type="button" variant="outline" size="sm" onClick={downloadTemplate}>
              <Download className="h-4 w-4" />
              Download template
            </Button>
            <input
              ref={fileRef}
              type="file"
              accept=".csv,text/csv"
              className="block w-full text-sm"
              onChange={(e) => setFile(e.target.files?.[0] ?? null)}
            />
          </div>
        ) : (
          <div className="space-y-3 text-sm">
            <p>
              <strong>{result.created}</strong> created · <strong>{result.duplicates}</strong> duplicates skipped ·{" "}
              <strong>{result.failed}</strong> failed · {result.totalRows} rows read
            </p>
            {result.issues.length > 0 && (
              <ul className="max-h-48 space-y-1 overflow-y-auto rounded-md border border-border p-2 thin-scrollbar">
                {result.issues.map((i) => (
                  <li key={i.row} className="text-xs">
                    <span className="font-medium">Row {i.row}</span>
                    {i.name ? ` (${i.name})` : ""}: {i.reason}
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}

        <DialogFooter>
          <Button type="button" variant="outline" onClick={() => handleOpenChange(false)}>
            {result ? "Close" : "Cancel"}
          </Button>
          {!result && (
            <Button type="button" disabled={!file || upload.isPending} onClick={() => file && upload.mutate(file)}>
              <Upload className="h-4 w-4" />
              {upload.isPending ? "Importing…" : "Import"}
            </Button>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
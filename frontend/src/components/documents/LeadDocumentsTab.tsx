import { useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Download, FileText, Trash2, Upload } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/common/EmptyState";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { documentsApi } from "@/api/documents";
import { extractErrorMessage } from "@/api/client";
import { formatDate } from "@/lib/utils";
import { DOCUMENT_TYPES, DOCUMENT_TYPE_LABELS, type DocumentType } from "@/types/document";

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export function LeadDocumentsTab({ leadId }: { leadId: string }) {
  const queryClient = useQueryClient();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [uploadType, setUploadType] = useState<DocumentType>("OTHER");

  const docsQuery = useQuery({ queryKey: ["documents", "lead", leadId], queryFn: () => documentsApi.byLead(leadId) });

  const upload = useMutation({
    mutationFn: (file: File) => documentsApi.upload(leadId, file, uploadType),
    onSuccess: () => {
      toast.success("Document uploaded");
      queryClient.invalidateQueries({ queryKey: ["documents", "lead", leadId] });
    },
    onError: (error) => toast.error(extractErrorMessage(error, "Couldn't upload document")),
  });

  const remove = useMutation({
    mutationFn: (id: string) => documentsApi.remove(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["documents", "lead", leadId] }),
    onError: (error) => toast.error(extractErrorMessage(error, "Couldn't delete document")),
  });

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-end gap-2">
        <Select value={uploadType} onValueChange={(v) => setUploadType(v as DocumentType)}>
          <SelectTrigger className="w-44">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {DOCUMENT_TYPES.map((t) => (
              <SelectItem key={t} value={t}>
                {DOCUMENT_TYPE_LABELS[t]}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <input
          ref={fileInputRef}
          type="file"
          className="hidden"
          onChange={(e) => {
            const file = e.target.files?.[0];
            if (file) upload.mutate(file);
            e.target.value = "";
          }}
        />
        <Button size="sm" onClick={() => fileInputRef.current?.click()} disabled={upload.isPending}>
          <Upload className="h-4 w-4" />
          {upload.isPending ? "Uploading…" : "Upload"}
        </Button>
      </div>

      {docsQuery.isLoading ? (
        <div className="space-y-2">
          <Skeleton className="h-12 w-full" />
          <Skeleton className="h-12 w-full" />
        </div>
      ) : docsQuery.data && docsQuery.data.content.length > 0 ? (
        <div className="divide-y divide-border rounded-lg border border-border">
          {docsQuery.data.content.map((doc) => (
            <div key={doc.id} className="flex items-center justify-between gap-3 p-3">
              <div className="flex min-w-0 items-center gap-2.5">
                <FileText className="h-4 w-4 shrink-0 text-muted-foreground" />
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium">{doc.originalFileName}</p>
                  <p className="text-xs text-muted-foreground">
                    {DOCUMENT_TYPE_LABELS[doc.documentType]} · {formatSize(doc.sizeBytes)} ·{" "}
                    {formatDate(doc.createdAt)}
                  </p>
                </div>
              </div>
              <div className="flex shrink-0 items-center gap-1">
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={() => documentsApi.download(doc.id, doc.originalFileName)}
                  aria-label="Download"
                >
                  <Download className="h-4 w-4" />
                </Button>
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={() => remove.mutate(doc.id)}
                  disabled={remove.isPending}
                  aria-label="Delete"
                >
                  <Trash2 className="h-4 w-4 text-destructive" />
                </Button>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <EmptyState icon={FileText} title="No documents uploaded" description="ID proofs, marksheets, and agreements for this lead will show up here." />
      )}
    </div>
  );
}

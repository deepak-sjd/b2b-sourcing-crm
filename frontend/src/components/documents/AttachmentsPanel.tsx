import { useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Download, FileText, Trash2, Upload } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/common/EmptyState";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { documentsApi } from "@/api/documents";
import { extractErrorMessage } from "@/api/client";
import { formatDate } from "@/lib/utils";
import { DOCUMENT_TYPES, DOCUMENT_TYPE_LABELS, type DocumentParentType, type DocumentType } from "@/types/document";

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

interface AttachmentsPanelProps {
  parentType: DocumentParentType;
  parentId: string;
  defaultType?: DocumentType;
}

export function AttachmentsPanel({ parentType, parentId, defaultType = "OTHER" }: AttachmentsPanelProps) {
  const queryClient = useQueryClient();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [uploadType, setUploadType] = useState<DocumentType>(defaultType);
  const [description, setDescription] = useState("");
  const [dragOver, setDragOver] = useState(false);

  const queryKey = ["documents", parentType, parentId];
  const docsQuery = useQuery({ queryKey, queryFn: () => documentsApi.byParent(parentType, parentId) });

  const upload = useMutation({
    mutationFn: (file: File) => documentsApi.upload(parentType, parentId, file, uploadType, description || undefined),
    onSuccess: () => {
      toast.success("Document uploaded");
      setDescription("");
      queryClient.invalidateQueries({ queryKey });
    },
    onError: (error) => toast.error(extractErrorMessage(error, "Couldn't upload document")),
  });

  const remove = useMutation({
    mutationFn: (id: string) => documentsApi.remove(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey }),
    onError: (error) => toast.error(extractErrorMessage(error, "Couldn't delete document")),
  });

  function handleFile(file: File | undefined) {
    if (file) upload.mutate(file);
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center gap-2">
        <Select value={uploadType} onValueChange={(v) => setUploadType(v as DocumentType)}>
          <SelectTrigger className="w-48">
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
        <Input
          placeholder="Description (optional)"
          className="max-w-xs"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
      </div>

      <div
        className={`flex flex-col items-center justify-center gap-2 rounded-lg border-2 border-dashed p-6 text-center transition-colors ${
          dragOver ? "border-primary bg-accent/40" : "border-border"
        }`}
        onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
        onDragLeave={() => setDragOver(false)}
        onDrop={(e) => {
          e.preventDefault();
          setDragOver(false);
          handleFile(e.dataTransfer.files?.[0]);
        }}
      >
        <Upload className="h-5 w-5 text-muted-foreground" />
        <p className="text-sm text-muted-foreground">Drag a file here, or</p>
        <input
          ref={fileInputRef}
          type="file"
          className="hidden"
          onChange={(e) => {
            handleFile(e.target.files?.[0]);
            e.target.value = "";
          }}
        />
        <Button size="sm" variant="outline" onClick={() => fileInputRef.current?.click()} disabled={upload.isPending}>
          {upload.isPending ? "Uploading…" : "Browse files"}
        </Button>
        <p className="text-xs text-muted-foreground">PDF, Word, Excel, CSV, PNG, JPEG, TXT</p>
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
                    {doc.uploadedBy?.fullName ?? "Unknown"} · {formatDate(doc.createdAt)}
                  </p>
                  {doc.description && <p className="truncate text-xs text-muted-foreground">{doc.description}</p>}
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
                  onClick={() => {
                    if (window.confirm(`Delete "${doc.originalFileName}"? The file is kept, just hidden from this list.`)) {
                      remove.mutate(doc.id);
                    }
                  }}
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
        <EmptyState icon={FileText} title="No documents uploaded" description="Files attached here will show up in this list." />
      )}
    </div>
  );
}
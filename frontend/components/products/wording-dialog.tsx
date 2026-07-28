"use client";

import { useRef, useState } from "react";
import { Download, FileText, Sparkles, Upload } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import type { ProductDocumentResponse } from "@/lib/types";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { useAsync } from "@/hooks/useAsync";
import { useMutation } from "@/hooks/useMutation";
import { productService } from "@/services/productService";
import { isApiError } from "@/lib/http";

/**
 * Upload and download the policy-wording PDF(s) for a product version. The wording is the master
 * terms document a policy issued under this version is governed by.
 */
export function WordingDialog({
  productId,
  versionId,
  open,
  onOpenChange,
}: {
  productId: number;
  versionId: number;
  open: boolean;
  onOpenChange: (v: boolean) => void;
}) {
  const fileRef = useRef<HTMLInputElement>(null);
  const [downloading, setDownloading] = useState<number | null>(null);
  const [lastUpload, setLastUpload] = useState<ProductDocumentResponse | null>(null);
  const { data, loading, refetch } = useAsync(
    () => productService.listDocuments(productId, versionId),
    [productId, versionId, open],
    { enabled: open },
  );

  const upload = useMutation(
    (file: File) => productService.uploadDocument(productId, versionId, file),
    {
      successMessage: "Wording uploaded",
      onSuccess: (result) => {
        if (fileRef.current) fileRef.current.value = "";
        setLastUpload(result);
        refetch();
      },
    },
  );

  const onPick = () => {
    const file = fileRef.current?.files?.[0];
    if (!file) {
      toast.error("Choose a file first");
      return;
    }
    upload.run(file);
  };

  const download = async (docId: number, fileName: string) => {
    setDownloading(docId);
    try {
      const url = await productService.documentDownloadUrl(productId, versionId, docId);
      const a = document.createElement("a");
      a.href = url;
      a.download = fileName;
      a.click();
      URL.revokeObjectURL(url);
    } catch (e) {
      toast.error(isApiError(e) ? e.message : "Could not download");
    } finally {
      setDownloading(null);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>Policy wording</DialogTitle>
          <DialogDescription>
            Upload the policy-wording PDF for this product version. A text-based PDF is also
            auto-extracted and indexed so the AI can answer coverage questions from it — no separate
            paste needed.
          </DialogDescription>
        </DialogHeader>

        <div className="grid gap-4">
          <div className="flex items-end gap-2">
            <input
              ref={fileRef}
              type="file"
              accept="application/pdf,.pdf,image/*"
              className="border-input bg-background file:text-foreground w-full rounded-md border px-3 py-2 text-sm file:mr-3 file:cursor-pointer file:border-0 file:bg-transparent file:text-sm file:font-medium"
            />
            <Button onClick={onPick} disabled={upload.loading}>
              <Upload className="mr-1 h-4 w-4" />
              {upload.loading ? "Uploading…" : "Upload"}
            </Button>
          </div>

          {lastUpload ? (
            lastUpload.indexedSections && lastUpload.indexedSections > 0 ? (
              <p className="flex items-center gap-1.5 text-sm text-green-600 dark:text-green-500">
                <Sparkles className="h-4 w-4 shrink-0" />
                Indexed {lastUpload.indexedSections} section
                {lastUpload.indexedSections === 1 ? "" : "s"} — the AI can now answer from this wording.
              </p>
            ) : (
              <p className="text-muted-foreground text-sm">
                Stored for download. Not indexed for AI — no extractable text (a scanned image PDF
                needs OCR, or paste the text via “Knowledge”).
              </p>
            )
          ) : null}

          <div>
            <p className="text-muted-foreground mb-2 text-xs font-medium tracking-wide uppercase">
              Uploaded documents
            </p>
            {loading ? (
              <p className="text-muted-foreground text-sm">Loading…</p>
            ) : !data || data.length === 0 ? (
              <p className="text-muted-foreground text-sm">Nothing uploaded yet.</p>
            ) : (
              <ul className="divide-y">
                {data.map((doc) => (
                  <li key={doc.id} className="flex items-center gap-3 py-2">
                    <FileText className="text-muted-foreground h-4 w-4 shrink-0" />
                    <span className="min-w-0 flex-1 truncate text-sm font-medium">
                      {doc.fileName}
                    </span>
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={downloading === doc.id}
                      onClick={() => download(doc.id, doc.fileName)}
                    >
                      <Download className="mr-1 h-4 w-4" />
                      {downloading === doc.id ? "…" : "Download"}
                    </Button>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}

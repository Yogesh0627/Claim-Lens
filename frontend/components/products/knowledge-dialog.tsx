"use client";

import { useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { useAsync } from "@/hooks/useAsync";
import { useMutation } from "@/hooks/useMutation";
import { coverageService } from "@/services/coverageService";

export function KnowledgeDialog({
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
  const [text, setText] = useState("");
  const { data, loading, refetch } = useAsync(
    () => (open ? coverageService.status(productId, versionId) : Promise.resolve(null)),
    [open, productId, versionId],
  );

  const ingest = useMutation((body: string) => coverageService.ingest(productId, versionId, body), {
    onSuccess: (r) => {
      toast.success(`Ingested ${r.chunkCount} chunks`);
      setText("");
      refetch();
    },
  });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>Policy knowledge — version {versionId}</DialogTitle>
          <DialogDescription>
            Paste the policy wording. It is chunked and embedded so coverage questions can be
            answered from it. Re-ingesting replaces the existing knowledge.
          </DialogDescription>
        </DialogHeader>
        <p className="text-muted-foreground text-sm">
          {loading
            ? "Checking…"
            : data && data.chunkCount > 0
              ? `${data.chunkCount} chunks ingested${data.embeddingModel ? ` · ${data.embeddingModel}` : ""}`
              : "No policy wording ingested yet."}
        </p>
        <Textarea
          rows={12}
          value={text}
          onChange={(e) => setText(e.target.value)}
          placeholder="Paste the policy wording / product terms here…"
        />
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Close
          </Button>
          <Button onClick={() => ingest.run(text)} disabled={ingest.loading || !text.trim()}>
            {ingest.loading ? "Ingesting…" : "Ingest"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
